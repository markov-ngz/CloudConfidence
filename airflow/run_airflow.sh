source $HOME/Documents/setup/python_env/spark_env/bin/activate

export AIRFLOW_HOME=$( pwd )
export AIRFLOW__API__HOST=127.0.0.1
export AIRFLOW__SCHEDULER__SCHEDULER_HEALTH_CHECK_SERVER_HOST=127.0.0.1
export AIRFLOW__CELERY__FLOWER_HOST=127.0.0.1
export AIRFLOW__CORE__LOAD_EXAMPLES=False
airflow standalone
