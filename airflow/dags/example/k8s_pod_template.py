import pendulum
from pathlib import Path
from airflow.sdk import dag, task
from airflow.providers.cncf.kubernetes.operators.pod import KubernetesPodOperator
from airflow.providers.cncf.kubernetes.utils.pod_manager import OnFinishAction


POD_TEMPLATE_DIR = Path(__file__).parent / "templates"

@dag(
    dag_id="k8s_pod_template",
    schedule="@once",
    template_searchpath=[str(POD_TEMPLATE_DIR)],
    start_date=pendulum.datetime(2026, 3, 30, tz="UTC"),
    catchup=False,
    tags=["k8s", "taskflow"],
)
def k8s_pod_template():

    @task
    def produce_value() -> str:
        return "hello-from-airflow"

    xcom_value = produce_value()

    # 2. KubernetesPodOperator that reads the XCom via an env var
    debug_xcom = KubernetesPodOperator(
        task_id="debug_xcom",
        name="java-xcom-pod",
        pod_template_file= POD_TEMPLATE_DIR / "pod_template.yaml",
        # task-level env vars are MERGED on top of the template
        env_vars={"DATA_PATH": "{{ ti.xcom_pull(task_ids='produce_value') }}"},
        on_finish_action=OnFinishAction.DELETE_POD,
        get_logs=True,
        in_cluster=True,
    )

    # 3. Set dependency explicitly
    xcom_value >> debug_xcom


k8s_pod_template()
