# Useful when running airflow on local machine not containerized

source $HOME/Documents/setup/python_env/spark_env/bin/activate
export AIRFLOW_HOME=$( pwd )

kubectl config get-contexts

KUBE_CONFIG=$(kubectl config view --minify --flatten -o json)
K8S_CONN="k8s_conn"

# Remove it if it exists, then add it fresh
airflow connections delete "$K8S_CONN" || true

airflow connections add "$K8S_CONN" \
    --conn-type 'kubernetes' \
    --conn-extra "{\"kube_config\": $KUBE_CONFIG}"

# airflow connections test "$K8S_CONN" if enabled