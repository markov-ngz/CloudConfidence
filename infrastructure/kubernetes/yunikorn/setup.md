# Links
- https://yunikorn.apache.org/docs/
- https://spark.kubeflow.org/en/latest/user-guide/yunikorn-integration.html
# Steps
```
helm repo add yunikorn https://apache.github.io/yunikorn-release
helm repo update
helm install yunikorn yunikorn/yunikorn --namespace yunikorn
```

Access browser: `
kubectl port-forward svc/yunikorn-service 9889:9889 -n yunikorn
`
