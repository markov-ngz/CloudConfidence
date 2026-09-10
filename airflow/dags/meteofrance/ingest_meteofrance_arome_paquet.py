import pendulum
from datetime import timedelta

from airflow.sdk import Param, dag, task
from airflow.providers.standard.operators.trigger_dagrun import TriggerDagRunOperator

DEFAULT_ARGS = {
    "owner": "data-cc",
    "retries": 0,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(minutes=30),
}

PREVINUM = "DPPaquetAROME-OM"
MODEL = "AROME-OM-NCALED"
GRID = "0.025"
PACKAGES = "SP1,SP2,SP3"
SPARK_MODEL = "StgMeteoFrancePaquetModel"


@dag(
    dag_id="ingest_meteofrance_arome_paquet",
    schedule=timedelta(hours=6),
    start_date=pendulum.datetime(2026, 3, 30, tz="UTC"),
    catchup=False,
    max_active_runs=1,
    default_args=DEFAULT_ARGS,
    tags=["meteofrance", "ingestion", "arome"],
    params={
        "starttime": Param(default="", type=["string"]),  # 2026-09-01T23:00:00Z
        "endtime": Param(default="", type=["string"]),  # 2026-09-02T05:00:00Z
    },
)
def ingest_meteofrance_arome_paquet():

    @task
    def format_date_parameters(starttime, endtime):
        start_time = (
            starttime
            if starttime != ""
            else pendulum.now("UTC").subtract(hours=6).to_iso8601_string()
        )
        end_time = endtime if endtime != "" else pendulum.now("UTC").to_iso8601_string()
        return {"starttime": start_time, "endtime": end_time}


    triggers = TriggerDagRunOperator(
        task_id="trigger_ingest_meteofrance_paquet_arome",
        trigger_dag_id="ingest_meteofrance_paquet",
        wait_for_completion=True,
        poke_interval=30,
        deferrable=False,
        conf=            {
            "previnum": PREVINUM,
            "model": MODEL,
            "grid": GRID,
            "packagenames": PACKAGES,
            "starttime": "{{ task_instance.xcom_pull(task_ids='format_date_parameters')['starttime'] }}",
            "endtime": "{{ task_instance.xcom_pull(task_ids='format_date_parameters')['endtime'] }}",
            "spark_model": SPARK_MODEL,
        }
    )

    format_date_parameters(
        starttime="{{ params.starttime }}",
        endtime="{{ params.endtime }}",
    ) >> triggers


ingest_meteofrance_arome_paquet()
