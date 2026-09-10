"""
Ingestion Meteofrance Observation

1. Extract specified departments
2. Load to Datalake the extracted files
3. Archive
"""

from __future__ import annotations

import logging
from datetime import timedelta
from pathlib import Path
import pendulum
from airflow.sdk import dag, task, Param

from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator
from pydantic import BaseModel, ValidationError
from airflow.providers.standard.operators.trigger_dagrun import TriggerDagRunOperator

from airflow.providers.cncf.kubernetes.operators.spark_kubernetes import (
    SparkKubernetesOperator,
)

KUBE_CONN_ID = "k8s_conn"
AWS_CONN_ID = "CLOUDCONFIDENCE_S3_BUCKET_CRD"

POD_TEMPLATE_DIR = Path(__file__).parent / "templates"
EXTRACTION_POD_TEMPLATE_FILE = (
    POD_TEMPLATE_DIR / "extraction_meteofrance_observation_template.yaml"
)  # k8s pod operator does open() and do not rely on airflow template system
LOAD_SPARKAPP_TEMPLATE = "transformation_spark_template.yaml"

log = logging.getLogger(__name__)

DEFAULT_ARGS = {
    "owner": "data-cc",
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(minutes=30),
}

LANDING_ZONE = "s3://cloudconfidence414-landing-zone"
SOURCE_FOLDER = "extraction/meteofrance-observation"
ARCHIVE_FOLDER = "archive/extraction/meteofrance-observation"


class ExtractionResult(BaseModel):
    status: str
    object: str
    data: ExtractionData


class ExtractionData(BaseModel):
    failures: list[dict]  # tighten this if you know the failure shape
    successes: list[SunkFile]


class SunkFile(BaseModel):
    filename: str
    uri: str
    format: str


@dag(
    dag_id="ingest_meteofrance_observation",
    # schedule=timedelta(hours=6),
    catchup=False,
    max_active_runs=1,
    template_searchpath=[str(POD_TEMPLATE_DIR)],
    default_args=DEFAULT_ARGS,
    tags=["meteofrance", "ingestion"],
    params={
        "department_ids": Param(
            default=[988],
            type="array",
            items={"type": "integer"},
            description="Météo-France department IDs to extract",
        ),
        "csv_delimiter": Param(
            default=";",
            type="string",
            items={"type": "string"},
            description="delimiter of the csv of meteofrance observation package api. Note that it changes for OM/oversea",
        ),
        "spark_model": Param(
            default="StgMeteoFranceObservationModel",
            type="string",
            items={"type": "string"},
            description="Staging model to ingest data into",
        ),
    },
)
def ingest_meteofrance_observation():

    extraction_meteofrance_observation = KubernetesPodOperator(
        name="extraction_meteofrance_observation",
        task_id="extraction_meteofrance_observation",
        pod_template_file=str(EXTRACTION_POD_TEMPLATE_FILE),
        do_xcom_push=True,
        on_finish_action="delete_pod",
        get_logs=True,
        in_cluster=True,
        arguments=[
            "--etl.jobid=EXTRACT-MFAROME",
            "--etl.source.departmentids={{ params.department_ids | join('|') }}",
            "--etl.source.format=csv",
        ],
    )
    #
    stg_meteofrance_observation = SparkKubernetesOperator(
        namespace="airflow-tasks",  # mandatory to specify the ns from the object if k8s conn namespace is default and not the one the spark job is running
        task_id="stg_meteofrance_observation",
        application_file=LOAD_SPARKAPP_TEMPLATE,
        do_xcom_push=True,
        log_events_on_failure=True,
        get_logs=True,
    )

    archive_raw_files = TriggerDagRunOperator(
        task_id="archive_raw_files",
        trigger_dag_id="s3_move",
        wait_for_completion=True,
        poke_interval=10,
        reset_dag_run=True,  # allows re-runs safely
        conf=                {
            "aws_conn_id": AWS_CONN_ID,
            "source_s3_uri": f"{LANDING_ZONE}/{SOURCE_FOLDER}/",
            "dest_s3_uri": f"{LANDING_ZONE}/{ARCHIVE_FOLDER}/",
        }
    )

    (
        # Extract
        extraction_meteofrance_observation
        # Load
        >> stg_meteofrance_observation
        # Archive file
        >> archive_raw_files
    )


ingest_meteofrance_observation()
