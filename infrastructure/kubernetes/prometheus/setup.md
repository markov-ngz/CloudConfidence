helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

helm pull oci://ghcr.io/prometheus-community/charts/kube-prometheus-stack --version 88.6.3 --untar

Update values
```sh
    serviceMonitorSelectorNilUsesHelmValues: false
    podMonitorSelectorNilUsesHelmValues: false
```

```sh
helm install kube-prometheus-stack ./kube-prometheus-stack -f custom-values.yaml --namespace monitoring
```
or 
```
helm install kube-prometheus-stack oci://ghcr.io/prometheus-community/charts/kube-prometheus-stack \
  --version 88.6.3 \
  --set serviceMonitorSelectorNilUsesHelmValues=false \
  --set podMonitorSelectorNilUsesHelmValues=false \
  --namespace monitoring
```

## Sources 
- https://spark.kubeflow.org/en/latest/user-guide/monitoring-with-jmx-and-prometheus.html
- https://github.com/prometheus-community/helm-charts/tree/main/charts/kube-prometheus-stack

```
kubectl port-forward $(kubectl get pods --selector=app.kubernetes.io/name=grafana -n monitoring --output=jsonpath="{.items..metadata.name}") -n monitoring 3001:3000

# Visit:
http://localhost:3001
# Login: admin / prom-operator

# Then import dashboard inside Grafana https://grafana.com/grafana/dashboards/23304
# Dashboard ID is: 23304
```

```
kubectl port-forward -n monitoring prometheus-prometheus-stack-kube-prom-prometheus-0 9090

# Visit:
http://localhost:9090
```