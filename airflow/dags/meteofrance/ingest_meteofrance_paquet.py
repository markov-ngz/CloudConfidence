"""
Ingestion Meteofrance Paquet

1. Extract specified paquet
2. Convert .grib file to .parquet
3. Load to Datalake
4. Archive raw files extracted
"""

from __future__ import annotations

import logging
from datetime import timedelta
from pathlib import Path
import pendulum
from airflow.sdk import dag, task, Param
import json
from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator
from pydantic import BaseModel, ValidationError
from airflow.providers.standard.operators.trigger_dagrun import TriggerDagRunOperator

from airflow.providers.cncf.kubernetes.operators.spark_kubernetes import (
    SparkKubernetesOperator,
)

KUBE_CONN_ID = "k8s_conn"

POD_TEMPLATE_DIR = Path(__file__).parent / "templates"
EXTRACTION_POD_TEMPLATE = (
    POD_TEMPLATE_DIR / "extraction_meteofrance_paquet_template.yaml"
)
CONVERSION_POD_TEMPLATE = POD_TEMPLATE_DIR / "conversion_grib2_parquet.yaml"
LOAD_SPARKAPP_TEMPLATE = "transformation_spark_template.yaml"

LANDING_ZONE = "s3://cloudconfidence414-landing-zone"
SOURCE_FOLDER = "extraction/meteofrance-paquet"
CONVERTED_FOLDER="conversion/meteofrance-paquet/grib2-parquet"
RAW_ARCHIVE_FOLDER = "archive/meteofrance-paquet"
CONVERTED_ARCHIVE_FOLDER="archive/conversion/meteofrance-paquet/grib2-parquet"

log = logging.getLogger(__name__)

DEFAULT_ARGS = {
    "owner": "data-cc",
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(minutes=60),
}

AWS_CONN_ID = "CLOUDCONFIDENCE_S3_BUCKET_CRD"


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
    dag_id="ingest_meteofrance_paquet",
    schedule=None,  # triggered externally, no own schedule
    start_date=pendulum.datetime(2026, 3, 30, tz="UTC"),
    catchup=False,
    max_active_runs=5,
    template_searchpath=[str(POD_TEMPLATE_DIR)],
    default_args=DEFAULT_ARGS,
    tags=["meteofrance", "ingestion"],
    params={
        "starttime": Param(type=["string"]),  # default="2026-08-19T23:00:00Z"
        "endtime": Param(type=["string"]),  # default="2026-08-20T05:00:00Z",
        "previnum": Param(type="string"),  # default="DPPaquetAROME-OM",
        "model": Param(type="string"),  # default="AROME-OM-NCALED",
        "grid": Param(type="string"),  # default="0.025",
        "packagenames": Param(  # singular: one package per run
            type="string",
            description="comma separated list of package name ( eg : SP1,SP2,SP3 )",
        ),  # default="SP1",
        "spark_model": Param(type="string"),  # default="StgMeteoFrancePaquetModel",
    },
)
def ingest_meteofrance_paquet():

    @task
    def format_date_parameters(starttime=None, endtime=None):
        start_time = (
            starttime
            if isinstance(starttime, str)
            else pendulum.now("UTC").subtract(hours=6).to_iso8601_string()
        )
        end_time = (
            endtime
            if isinstance(endtime, str)
            else pendulum.now("UTC").to_iso8601_string()
        )
        return {"starttime": start_time, "endtime": end_time}

    extraction_meteofrance_paquet = KubernetesPodOperator(
        kubernetes_conn_id=KUBE_CONN_ID,
        task_id="extraction_meteofrance_paquet",
        name="extraction_meteofrance_paquet",
        pod_template_file=str(EXTRACTION_POD_TEMPLATE),
        do_xcom_push=True,
        on_finish_action="keep_pod",
        in_cluster=True,
        arguments=[
            "--etl.jobid=EXTRACT-PAQUET",
            "--etl.source.previnum={{ params.previnum }}",
            "--etl.source.model={{ params.model }}",
            "--etl.source.grid={{ params.grid }}",
            "--etl.source.packagenames={{ params.packagename }}",  # singular param
            "--etl.source.starttime={{ task_instance.xcom_pull(task_ids='format_date_parameters')['starttime'] }}",
            "--etl.source.endtime={{ task_instance.xcom_pull(task_ids='format_date_parameters')['endtime'] }}",
        ],
        get_logs=True,
    )

    conversion_grib2_parquet = KubernetesPodOperator(
        kubernetes_conn_id=KUBE_CONN_ID,
        task_id="conversion_grib2_parquet",
        name="conversion_grib2_parquet",
        pod_template_file=str(CONVERSION_POD_TEMPLATE),
        do_xcom_push=True,
        on_finish_action="keep_pod",
        get_logs=True,
        in_cluster=True,
        arguments=[
            "--etl.jobrunid={{ run_id }}",
            '--etl.sunkfiles={{ task_instance.xcom_pull(task_ids="extraction_meteofrance_paquet")["data"]["successes"] | tojson }}',
        ],
    )

    stg_meteofrance_paquet = SparkKubernetesOperator(
        kubernetes_conn_id=KUBE_CONN_ID,
        namespace="airflow-tasks",
        task_id="stg_meteofrance_paquet",
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
            "dest_s3_uri": f"{LANDING_ZONE}/{RAW_ARCHIVE_FOLDER}/",
        }
    )

    archive_converted_files = TriggerDagRunOperator(
        task_id="archive_converted_files",
        trigger_dag_id="s3_move",
        wait_for_completion=True,
        poke_interval=10,
        reset_dag_run=True,  # allows re-runs safely
        conf=                {
            "aws_conn_id": AWS_CONN_ID,
            "source_s3_uri": f"{LANDING_ZONE}/{CONVERTED_FOLDER}/",
            "dest_s3_uri": f"{LANDING_ZONE}/{CONVERTED_ARCHIVE_FOLDER}/",
        }
    )

    (
        format_date_parameters(
            starttime="{{ params.starttime }}",
            endtime="{{ params.endtime }}",
        )
        >> extraction_meteofrance_paquet
        >> conversion_grib2_parquet
        >> stg_meteofrance_paquet
    )
    stg_meteofrance_paquet >> archive_raw_files
    stg_meteofrance_paquet >> archive_converted_files


ingest_meteofrance_paquet()
