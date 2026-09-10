NAMESPACE=polaris

helm pull polaris \
  --repo https://downloads.apache.org/polaris/helm-chart \
  --devel \
  --untar \
  --untardir ./helm


kubectl apply -f 0_polaris_db

kubectl create secret generic polaris-persistence \
  --namespace $NAMESPACE \
  --from-literal=username=postgres \
  --from-literal=password=postgres\
  --from-literal=jdbcUrl=jdbc:postgresql://postgres.polaris.svc.cluster.local:5432/POLARIS

helm upgrade --install --namespace $NAMESPACE \
  -f ./custom_values.yaml \
  polaris helm/polaris

kubectl wait --namespace $NAMESPACE --for=condition=ready pod --selector=app.kubernetes.io/name=polaris --timeout=30s

kubectl run polaris-bootstrap \
  -n $NAMESPACE \
  --image=apache/polaris-admin-tool:latest \
  --restart=Never \
  --rm -it \
  --env="quarkus.datasource.username=$(kubectl get secret polaris-persistence -n $NAMESPACE -o jsonpath='{.data.username}' | base64 --decode)" \
  --env="quarkus.datasource.password=$(kubectl get secret polaris-persistence -n $NAMESPACE -o jsonpath='{.data.password}' | base64 --decode)" \
  --env="quarkus.datasource.jdbc.url=$(kubectl get secret polaris-persistence -n $NAMESPACE -o jsonpath='{.data.jdbcUrl}' | base64 --decode)" \
  -- \
  bootstrap -r POLARIS -c POLARIS,root,s3cr3t -p
