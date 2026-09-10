import pendulum
from airflow.sdk import dag, task
from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator

@dag(
    dag_id="k8s_pod_write_xcom",
    schedule="@once",
    start_date=pendulum.datetime(2026, 3, 30, tz="UTC"),
    catchup=False,
    tags=["k8s", "taskflow"],
)
def k8s_pod_write_xcom():

    # 1. KubernetesPodOperator
    write_xcom = KubernetesPodOperator(
        task_id="write_xcom",
        name="write-xcom",
        namespace="airflow-tasks",
        image="alpine",
        cmds=[
            "sh",
            "-c",
            "mkdir -p /airflow/xcom/; echo '[1,2,3,4]' > /airflow/xcom/return.json",
        ],
        do_xcom_push=True,
        on_finish_action="delete_pod",
        get_logs=True,
        in_cluster=True,
    )

    # 2. Modern Python task instead of BashOperator
    @task
    def process_pod_result(data):
        if data and isinstance(data, list) and len(data) > 0:
            print(f"The first value is: {data[0]}")
            return data[0]
        return "No data"

    # 3. Set dependencies using functional calls
    process_pod_result(write_xcom.output)


k8s_pod_write_xcom()
