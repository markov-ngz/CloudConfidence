"""
s3_cleanup
----------
Weekly DAG that deletes all S3 objects under a given prefix that are
older than a configurable retention period (default: 2 weeks).

Design notes
------------
- Listing is done with paginator directly (boto3 via hook) so that
  buckets with millions of objects are handled without loading everything
  into memory at once.
- Deletion uses S3's native batch delete (up to 1 000 keys per request)
  to minimise API calls.
- The cutoff date is fixed at task-start time and passed explicitly so
  that retries are deterministic (same cutoff, same files deleted).
- `delete_objects` is intentionally a single task (not mapped) because
  S3 batch delete is already the efficient primitive here — spawning one
  task per file would be wasteful.
- Folder (prefix) and retention days are Params so the DAG can be
  reused for other prefixes via manual trigger or config override.
"""

from __future__ import annotations

import logging
from datetime import UTC, datetime, timedelta

import pendulum
from airflow.decorators import dag, task
from airflow.exceptions import AirflowSkipException
from airflow.models.param import Param
from airflow.providers.amazon.aws.hooks.s3 import S3Hook

log = logging.getLogger(__name__)

_S3_DELETE_BATCH_SIZE = 1_000   # hard AWS limit per delete_objects call


# ─────────────────────────────────────────────────────────────────────────────
# DAG
# ─────────────────────────────────────────────────────────────────────────────

@dag(
    dag_id="s3_cleanup",
    description=(
            "Delete S3 objects older than retention_days under a given prefix, "
            "every week."
    ),
    schedule="@weekly",
    start_date=pendulum.datetime(2026, 1, 1, tz="UTC"),
    catchup=False,
    render_template_as_native_obj=False,
    tags=["s3", "cleanup"],
    # Retries at DAG level: if listing or deletion fails mid-way, retry once
    # after 5 min so transient S3 throttling is handled automatically.
    default_args={
        "retries": 1,
        "retry_delay": timedelta(minutes=5),
    },
    params={
        "aws_conn_id": Param(
            default="aws_default",
            type="string",
            title="AWS connection id",
            description="Airflow id of the AWS connection to use",
        ),
        "s3_uri": Param(
            default="s3://my-bucket/archive/",
            type="string",
            title="S3 folder URI",
            description=(
                    "Prefix to clean up. Must end with '/'. "
                    "Recursive — all objects under this prefix are considered."
            ),
        ),
        "retention_days": Param(
            default=14,
            type="integer",
            title="Retention (days)",
            description="Objects last modified more than this many days ago will be deleted.",
        ),
    },
)
def s3_cleanup():

    # ── Step 1: Resolve cutoff timestamp ─────────────────────────────────────
    @task
    def compute_cutoff(**context) -> dict:
        """
        Compute and return the cutoff datetime + parsed bucket/prefix.

        Pinning the cutoff here (rather than inside list_expired_keys)
        means a retry of the delete step uses the exact same cutoff as
        the listing step that produced the keys — no accidental drift.
        """
        params = context["params"]
        s3_uri = params["s3_uri"]

        if not s3_uri.endswith("/"):
            raise ValueError(
                f"s3_uri {s3_uri!r} does not end with '/'. "
                "This DAG only accepts folder prefixes."
            )

        from urllib.parse import urlparse
        parsed = urlparse(s3_uri)
        if parsed.scheme != "s3":
            raise ValueError(f"s3_uri must start with s3://, got: {s3_uri!r}")

        bucket = parsed.netloc
        prefix = parsed.path.lstrip("/")

        if not bucket:
            raise ValueError(f"Could not extract bucket from: {s3_uri!r}")

        retention_days: int = params["retention_days"]
        # Use the *logical* run date as anchor so weekly reruns are idempotent
        # (re-running the same DAG interval always targets the same window).
        logical_date: datetime = context["logical_date"]
        cutoff: datetime = logical_date - timedelta(days=retention_days)

        log.info(
            "Cleanup config | bucket: %s | prefix: %s | "
            "retention: %d days | cutoff: %s",
            bucket, prefix, retention_days, cutoff.isoformat(),
        )

        return {
            "bucket":         bucket,
            "prefix":         prefix,
            "cutoff_iso":     cutoff.isoformat(),   # XCom-safe (str, not datetime)
            "retention_days": retention_days,
        }

    # ── Step 2: Page through S3 and collect expired keys ─────────────────────
    @task
    def list_expired_keys(config: dict, **context) -> list[str]:
        """
        Page through every object under prefix (fully recursive) and
        return the keys whose LastModified is older than the cutoff.

        Uses the boto3 paginator directly so that listing is streaming —
        only the final key list is held in memory, not intermediate pages.
        This safely handles prefixes with millions of objects.
        """
        aws_conn_id = context["params"]["aws_conn_id"]
        hook = S3Hook(aws_conn_id=aws_conn_id)

        bucket    = config["bucket"]
        prefix    = config["prefix"]
        cutoff    = datetime.fromisoformat(config["cutoff_iso"])

        # Make cutoff timezone-aware (S3 LastModified is always UTC-aware)
        if cutoff.tzinfo is None:
            cutoff = cutoff.replace(tzinfo=UTC)

        s3_client = hook.get_conn()
        paginator = s3_client.get_paginator("list_objects_v2")

        expired_keys: list[str] = []
        total_scanned = 0

        for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
            objects = page.get("Contents", [])
            total_scanned += len(objects)
            for obj in objects:
                if obj["LastModified"] < cutoff:
                    expired_keys.append(obj["Key"])

        log.info(
            "Scanned %d objects | %d older than cutoff (%s) | %d will be kept",
            total_scanned,
            len(expired_keys),
            cutoff.isoformat(),
            total_scanned - len(expired_keys),
            )

        if not expired_keys:
            raise AirflowSkipException(
                f"No objects older than {config['retention_days']} days found. "
                "Nothing to delete."
            )

        return expired_keys

    # ── Step 3: Batch-delete expired keys ────────────────────────────────────
    @task
    def delete_expired_keys(keys: list[str], config: dict, **context) -> dict:
        """
        Delete all expired keys using S3 native batch delete.
        Sends keys in batches of 1 000 (the AWS hard limit per request).

        Returns a summary dict pushed to XCom for auditability.
        """
        aws_conn_id = context["params"]["aws_conn_id"]
        hook = S3Hook(aws_conn_id=aws_conn_id)

        bucket = config["bucket"]
        s3_client = hook.get_conn()

        total      = len(keys)
        deleted    = 0
        errors     = []

        # Chunk into batches of 1 000
        batches = [
            keys[i : i + _S3_DELETE_BATCH_SIZE]
            for i in range(0, total, _S3_DELETE_BATCH_SIZE)
        ]

        log.info("Deleting %d objects in %d batch(es)...", total, len(batches))

        for batch_num, batch in enumerate(batches, start=1):
            response = s3_client.delete_objects(
                Bucket=bucket,
                Delete={
                    "Objects": [{"Key": k} for k in batch],
                    "Quiet": False,   # get explicit per-key success/error back
                },
            )

            batch_deleted = len(response.get("Deleted", []))
            batch_errors  = response.get("Errors", [])

            deleted += batch_deleted
            errors.extend(batch_errors)

            log.info(
                "Batch %d/%d | deleted: %d | errors: %d",
                batch_num, len(batches), batch_deleted, len(batch_errors),
            )

            if batch_errors:
                for err in batch_errors:
                    log.error(
                        "Failed to delete s3://%s/%s | code: %s | message: %s",
                        bucket, err.get("Key"), err.get("Code"), err.get("Message"),
                    )

        summary = {
            "bucket":      bucket,
            "prefix":      config["prefix"],
            "cutoff":      config["cutoff_iso"],
            "total_found": total,
            "deleted":     deleted,
            "failed":      len(errors),
            "errors":      errors,
        }

        log.info("Cleanup summary: %s", summary)

        # Fail the task if any key could not be deleted so the retry
        # policy kicks in and the operator is alerted.
        if errors:
            raise RuntimeError(
                f"{len(errors)} object(s) could not be deleted. "
                "See task logs for details. The task will retry."
            )

        return summary

    # ── Wire up ───────────────────────────────────────────────────────────────
    config = compute_cutoff()
    keys   = list_expired_keys(config)
    delete_expired_keys(keys, config)


s3_cleanup()