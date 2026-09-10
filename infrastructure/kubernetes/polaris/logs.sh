export pod_name=$(kubectl get pod -n polaris -l app.kubernetes.io/name=polaris -o jsonpath='{.items[0].metadata.name}')
kubectl logs --tail 100 $pod_name