import pendulum
from airflow.sdk import dag, task
from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator
from airflow.providers.cncf.kubernetes.utils.pod_manager import OnFinishAction

KUBE_CONN_ID = "k8s_conn2"


@dag(
    dag_id="k8s_pod_read_xcom",
    schedule="@once",
    start_date=pendulum.datetime(2026, 3, 30, tz="UTC"),
    catchup=False,
    tags=["k8s", "taskflow"],
)
def k8s_pod_read_xcom():

    # 1. Simple Python task that produces an XCom
    @task
    def produce_value() -> str:
        return "hello-from-airflow"

    xcom_value = produce_value()

    # 2. KubernetesPodOperator that reads the XCom via an env var
    read_xcom = KubernetesPodOperator(
        task_id="read_xcom",
        name="java-xcom-pod",
        namespace="airflow-tasks",
        image="check-xcom:0.1.0",
        do_xcom_push=False,
        on_finish_action=OnFinishAction.DELETE_POD,
        kubernetes_conn_id=KUBE_CONN_ID,
        get_logs=True,
        in_cluster=True,
        env_vars={
            "XCOM_VALUE": "{{ ti.xcom_pull(task_ids='produce_value') }}"
        },
    )

    # 3. Set dependency explicitly
    xcom_value >> read_xcom


k8s_pod_read_xcom()
