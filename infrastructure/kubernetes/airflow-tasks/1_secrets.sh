kubectl create secret generic aws-credentials \
    --from-file=aws_access_key_id \
    --from-file=aws_secret_access_key \
    --from-file=aws_region \
    --namespace airflow-tasks

kubectl create secret generic polaris-credentials \
    --from-file=../polaris/client_credentials \
    --namespace airflow-tasks

kubectl create secret generic meteofrance-api \
    --from-file=$HOME/meteofrance-api/application_id \
    --namespace airflow-tasks