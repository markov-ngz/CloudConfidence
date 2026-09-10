## Steps
```sh
helm repo add apache-airflow https://airflow.apache.org

helm search repo apache-airflow --versions

helm pull apache-airflow/airflow --version 1.22.0 --untar

helm upgrade --install --namespace airflow \
  -f ./custom_values.yaml \
  airflow ./airflow
```

## Git 
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: airflow-ssh-secret
  namespace: airflow
data:
  gitSshKey: <base64 sshkey>
```

## Sources

https://airflow.apache.org/docs/helm-chart/stable/production-guide.html#values-file
https://airflow.apache.org/docs/helm-chart/stable/index.html#installing-the-helm-chart-with-argo-cd-flux-rancher-or-terraform
https://airflow.apache.org/docs/helm-chart/stable/manage-dag-files.html

To forward : 
```sh
kubectl port-forward -n airflow svc/airflow-api-server 8080:8080
```
