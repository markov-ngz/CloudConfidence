
NAMESPACE=polaris

kubectl delete ns $NAMESPACE

PV_TO_DELETE=$(kubectl get pv -o name -n $NAMESPACE)

for pv in $PV_TO_DELETE; do
  echo "Deleting PV: $pv"
  kubectl delete "$pv"
done