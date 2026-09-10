# Cert manager is mandatory for webhook
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.21.0/cert-manager.yaml

# Download helm to check crds & resources
helm pull spark-operator \
    --repo https://kubeflow.github.io/spark-operator \
    --untar \
    --untardir ./helm

helm upgrade --install --namespace spark-operator  \
  --set "controller.batchScheduler.enable=true" \
  --set "controller.batchScheduler.default=yunikorn" \
  --set "spark.jobNamespaces={airflow-tasks,spark-operator}" \
  spark-operator helm/spark-operator