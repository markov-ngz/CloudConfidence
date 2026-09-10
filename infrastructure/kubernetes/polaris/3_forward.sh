pod_name=$(kubectl get pod -n polaris -l app.kubernetes.io/name=polaris -o jsonpath='{.items[0].metadata.name}')
kubectl port-forward -n polaris $pod_name 8181:8181