import pendulum
from airflow.sdk import dag, task
from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator

@dag(
    dag_id="java_kubernetes_pod",
    schedule="@once",
    start_date=pendulum.datetime(2026, 3, 30, tz="UTC"),
    catchup=False,
    tags=["k8s", "taskflow"],
)
def kubernetes_workflow():

    # 1. KubernetesPodOperator
    write_xcom = KubernetesPodOperator(
        task_id="write_xcom",
        name="java-xcom-pod",
        namespace="airflow-tasks",
        image="check-xcom:0.1.0",
        do_xcom_push=True,
        on_finish_action="delete_pod",
        get_logs=True,
        in_cluster=True,
    )

    # 2. Modern Python task instead of BashOperator
    @task
    def process_pod_result(data):
        print(data)
        print(type(data))
        print(data.keys())

    # 3. Set dependencies using functional calls
    process_pod_result(write_xcom.output)


kubernetes_workflow()
