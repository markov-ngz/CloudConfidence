"""
s3_move
-------
Single DAG that handles both cases:
  - File-to-file  : s3://bucket/prefix/file.csv   → s3://bucket/prefix/file.csv
  - Folder-to-folder: s3://bucket/prefix/folder/  → s3://bucket/prefix/folder/

Rules
-----
- Trailing '/' on source URI means folder mode.
- If source is a folder, destination MUST also end with '/'.
- Folder mode moves only level-1 files (no recursion).
- File mode rejects folder URIs immediately.
"""

from __future__ import annotations

import logging
from urllib.parse import urlparse

import pendulum
from airflow.sdk import dag, task, Param
from airflow.exceptions import AirflowSkipException
from airflow.providers.amazon.aws.hooks.s3 import S3Hook

log = logging.getLogger(__name__)


# ─────────────────────────────────────────────────────────────────────────────
# Module-level helpers  (importable by other DAGs if needed)
# ─────────────────────────────────────────────────────────────────────────────

def _parse_s3_uri(uri: str) -> tuple[str, str]:
    parsed = urlparse(uri)
    if parsed.scheme != "s3":
        raise ValueError(f"URI must start with s3://, got: {uri!r}")
    bucket = parsed.netloc
    key = parsed.path.lstrip("/")
    if not bucket:
        raise ValueError(f"Could not extract bucket from URI: {uri!r}")
    return bucket, key


def _is_folder_uri(uri: str) -> bool:
    return uri.endswith("/")


# ─────────────────────────────────────────────────────────────────────────────
# Shared atomic operation  (the logic you wanted to reuse)
# ─────────────────────────────────────────────────────────────────────────────

def _move_single_file(
        aws_conn_id: str,
        src_bucket: str,
        src_key: str,
        dst_bucket: str,
        dst_key: str,
) -> None:
    """
    Copy then delete one S3 object.
    Plain function — no Airflow coupling — so it is safely callable
    from any @task without dragging in DAG/operator machinery.
    """
    hook = S3Hook(aws_conn_id=aws_conn_id)

    hook.copy_object(
        source_bucket_key=src_key,
        dest_bucket_key=dst_key,
        source_bucket_name=src_bucket,
        dest_bucket_name=dst_bucket,
    )
    log.info("Copied  s3://%s/%s  →  s3://%s/%s", src_bucket, src_key, dst_bucket, dst_key)

    hook.delete_objects(bucket=src_bucket, keys=[src_key])
    log.info("Deleted s3://%s/%s", src_bucket, src_key)


# ─────────────────────────────────────────────────────────────────────────────
# DAG
# ─────────────────────────────────────────────────────────────────────────────

@dag(
    dag_id="s3_move",
    description="Move a file or all level-1 files in a folder between S3 locations",
    schedule=None,
    start_date=pendulum.datetime(2026, 1, 1, tz="UTC"),
    catchup=False,
    render_template_as_native_obj=False,
    tags=["s3"],
    params={
        "aws_conn_id": Param(
            type="string",
            title="AWS connection id",
            description="Airflow id of the AWS connection to use",
        ),
        "source_s3_uri": Param(
            default="s3://my-bucket/input/folder/file.csv",
            type="string",
            title="Source S3 URI",
            description=(
                    "File: s3://bucket/prefix/file.csv  "
                    "Folder: s3://bucket/prefix/folder/  (trailing slash required)"
            ),
        ),
        "dest_s3_uri": Param(
            default="s3://my-bucket/output/folder/file.csv",
            type="string",
            title="Destination S3 URI",
            description=(
                    "Must match the source type: "
                    "file→file key, folder→folder prefix (trailing slash)"
            ),
        ),
    },
)
def s3_move():

    # ── Step 1: Validate and parse — fails fast before any S3 call ───────────
    @task
    def parse_uris(**context) -> dict:
        """
        Validates the URI pair, detects file vs folder mode, and returns
        a descriptor that drives all downstream tasks.

        Returned dict:
            mode        : "file" | "folder"
            src_bucket  : str
            src_prefix  : str   (key for file, prefix for folder)
            dst_bucket  : str
            dst_prefix  : str   (key for file, prefix for folder)
        """
        params = context["params"]
        src_uri = params["source_s3_uri"]
        dst_uri = params["dest_s3_uri"]

        src_is_folder = _is_folder_uri(src_uri)
        dst_is_folder = _is_folder_uri(dst_uri)

        # ── The one combined guard that covers both DAGs' validation ─────────
        if src_is_folder and not dst_is_folder:
            raise ValueError(
                f"Source {src_uri!r} is a folder (ends with '/') but "
                f"destination {dst_uri!r} is not. "
                "Both must be folder URIs when moving a folder."
            )

        if not src_is_folder and dst_is_folder:
            raise ValueError(
                f"Source {src_uri!r} is a file but "
                f"destination {dst_uri!r} is a folder (ends with '/'). "
                "Provide a full destination file key, not a prefix."
            )

        src_bucket, src_prefix = _parse_s3_uri(src_uri)
        dst_bucket, dst_prefix = _parse_s3_uri(dst_uri)
        mode = "folder" if src_is_folder else "file"

        log.info(
            "Mode: %s | s3://%s/%s  →  s3://%s/%s",
            mode, src_bucket, src_prefix, dst_bucket, dst_prefix,
        )

        return {
            "mode":       mode,
            "src_bucket": src_bucket,
            "src_prefix": src_prefix,
            "dst_bucket": dst_bucket,
            "dst_prefix": dst_prefix,
        }

    # ── Step 2 (folder only): list level-1 files ─────────────────────────────
    @task
    def list_files(parsed: dict, **context) -> list[dict]:
        """
        In file mode  → returns a single-element list so that copy_and_delete
                         can be uniformly mapped in both modes.
        In folder mode → lists level-1 keys and expands into one descriptor
                         per file. Raises AirflowSkipException if empty.

        Every descriptor is:
            { src_bucket, src_key, dst_bucket, dst_key }
        """
        aws_conn_id = context["params"]["aws_conn_id"]

        if parsed["mode"] == "file":
            # File mode: no S3 listing needed, wrap the single file directly
            return [
                {
                    "src_bucket": parsed["src_bucket"],
                    "src_key":    parsed["src_prefix"],   # key, not prefix
                    "dst_bucket": parsed["dst_bucket"],
                    "dst_key":    parsed["dst_prefix"],
                }
            ]

        # Folder mode
        hook = S3Hook(aws_conn_id=aws_conn_id)
        src_bucket = parsed["src_bucket"]
        src_prefix = parsed["src_prefix"]
        dst_bucket = parsed["dst_bucket"]
        dst_prefix = parsed["dst_prefix"]

        all_keys: list[str] = hook.list_keys(
            bucket_name=src_bucket,
            prefix=src_prefix,
        ) or []

        # Level-1 only: nothing after the prefix may contain a '/'
        level1_keys = [
            k for k in all_keys
            if k != src_prefix
               and "/" not in k[len(src_prefix):]
        ]

        if not level1_keys:
            log.warning("No level-1 files found under s3://%s/%s", src_bucket, src_prefix)
            raise AirflowSkipException("No files to move.")

        transfers = [
            {
                "src_bucket": src_bucket,
                "src_key":    src_key,
                "dst_bucket": dst_bucket,
                "dst_key":    dst_prefix + src_key[len(src_prefix):],
            }
            for src_key in level1_keys
        ]

        log.info("Files to move (%d):", len(transfers))
        for t in transfers:
            log.info(
                "  s3://%s/%s  →  s3://%s/%s",
                t["src_bucket"], t["src_key"],
                t["dst_bucket"], t["dst_key"],
            )

        return transfers

    # ── Step 3: Copy + delete each file (mapped uniformly for both modes) ────
    @task
    def copy_and_delete(transfer: dict, **context) -> None:
        """
        Calls the shared _move_single_file helper.
        Mapped over the list produced by list_files so that:
          - file mode  → exactly one task instance
          - folder mode → one task instance per level-1 file
        Per-file retry, per-file failure visibility, zero code duplication.
        """
        _move_single_file(
            aws_conn_id=context["params"]["aws_conn_id"],
            src_bucket=transfer["src_bucket"],
            src_key=transfer["src_key"],
            dst_bucket=transfer["dst_bucket"],
            dst_key=transfer["dst_key"],
        )

    # ── Wire up ───────────────────────────────────────────────────────────────
    parsed    = parse_uris()
    transfers = list_files(parsed)
    copy_and_delete.expand(transfer=transfers)


s3_move()