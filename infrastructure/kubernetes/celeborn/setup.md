```
helm pull oci://ghcr.io/prometheus-community/charts/kube-prometheus-stack --version 88.6.3 --untar

# Download raw repo 
wget https://github.com/apache/celeborn/archive/refs/tags/v0.7.0.zip
# Unzip
unzip v0.7.0.zip
# Extract the helm chart
mv celeborn-0.7.0/charts/celeborn celeborn
# Remove all other folders
rm -r celeborn-0.7.0
rm v0.7.0.zip

# Install helm chart
helm install celeborn -n celeborn ./celeborn

# Update if misconfig
helm update celeborn -n celeborn ./celeborn
```
doc :
- https://github.com/apache/celeborn#spark-configuration
- https://celeborn.apache.org/docs/latest/deploy_on_k8s/
