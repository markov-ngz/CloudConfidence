from __future__ import annotations

import logging
from datetime import timedelta
from pathlib import Path
import pendulum
from airflow.sdk import dag, task, Param, Variable

from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator
from pydantic import BaseModel, ValidationError

from airflow.providers.cncf.kubernetes.operators.spark_kubernetes import (
    SparkKubernetesOperator,
)

KUBE_CONN_ID = "k8s_conn"

POD_TEMPLATE_DIR = Path(__file__).parent / "templates"
SPARKAPP_TEMPLATE = "transformation_spark_template.yaml"

log = logging.getLogger(__name__)

DEFAULT_ARGS = {
    "owner": "data-cc",
    "retries": 0,
    "execution_timeout": timedelta(minutes=30),
}


@dag(
    dag_id="transform_forecast_error_over_time",
    catchup=False,
    max_active_runs=1,
    template_searchpath=[str(POD_TEMPLATE_DIR)],
    default_args=DEFAULT_ARGS,
    tags=["cloudconfidence"],
)
def transform_forecast_error_over_time():

    int_station_forecast_observation = SparkKubernetesOperator(
        kubernetes_conn_id=KUBE_CONN_ID,
        namespace="airflow-tasks",
        task_id="int_station_forecast_observation",
        application_file=SPARKAPP_TEMPLATE,
        do_xcom_push=True,
        log_events_on_failure=True,
        get_logs=True,
        params={"model_name": "IntStationForecastObservationModel"},
    )

    mart_forecast_error_over_time = SparkKubernetesOperator(
        kubernetes_conn_id=KUBE_CONN_ID,
        namespace="airflow-tasks",
        task_id="mart_forecast_error_over_time",
        application_file=SPARKAPP_TEMPLATE,
        do_xcom_push=True,
        log_events_on_failure=True,
        get_logs=True,
        params={"model_name": "MartForecastErrorOverTimeModel"},
    )

    int_station_forecast_observation >> mart_forecast_error_over_time


transform_forecast_error_over_time()
