# Steps
```
helm repo add fluent https://fluent.github.io/helm-charts

helm install fluent-bit  -f values.yaml  -n fluent-bit ./fluent-bit

helm upgrade fluent-bit  -f values.yaml  -n fluent-bit ./fluent-bit

export POD_NAME=$(kubectl get pods --namespace fluent-bit -l "app.kubernetes.io/name=fluent-bit,app.kubernetes.io/instance=fluent-bit" -o jsonpath="{.items[0].metadata.name}")
kubectl logs $POD_NAME -n fluent-bit > log_fluent_bit_2

helm uninstall fluent-bit  -f values.yaml  -n fluent-bit ./fluent-bit
```

doc : https://docs.fluentbit.io/manual/installation/downloads/kubernetes
