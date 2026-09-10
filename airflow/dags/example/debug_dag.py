from airflow.decorators import dag, task
from datetime import datetime


@dag(
    start_date=datetime(2025, 9, 1),
    catchup=False,
    tags=["tutorial"],
)
def debug_dag():
    @task
    def test():
        return "Hello, Airflow!"

    test()


debug_dag = debug_dag()
