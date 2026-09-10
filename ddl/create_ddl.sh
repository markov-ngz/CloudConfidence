source $HOME/Documents/setup/python_env/spark_env/bin/activate

source aws_creds.sh

export POLARIS_CREDENTIAL=$(cat  ../kubernetes/polaris/client_credentials)
export CATALOG_NAME=dev_catalog

spark-sql \
  --master local[*] \
  --jars spark_jars/iceberg-aws-bundle-1.11.0.jar,spark_jars/iceberg-spark-runtime-4.1_2.13-1.11.0.jar \
  --conf spark.driver.host=127.0.0.1 \
  --conf spark.driver.bindAddress=127.0.0.1 \
  --conf spark.local.ip=127.0.0.1 \
  --conf spark.ui.host=127.0.0.1 \
  --conf spark.ui.bindAddress=127.0.0.1 \
  --conf spark.sql.extensions="org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions" \
  --conf spark.sql.defaultCatalog="rest" \
  --conf spark.sql.catalog.rest="org.apache.iceberg.spark.SparkCatalog" \
  --conf spark.sql.catalog.rest.type="rest" \
  --conf spark.sql.catalog.rest.uri="http://localhost:8181/api/catalog" \
  --conf spark.sql.catalog.rest.warehouse=$CATALOG_NAME \
  --conf spark.sql.catalog.rest.auth.type="oauth2" \
  --conf spark.sql.catalog.rest.oauth2-server-uri="http://localhost:8181/api/catalog/v1/oauth/tokens" \
  --conf spark.sql.catalog.rest.scope="PRINCIPAL_ROLE:ALL" \
  --conf spark.sql.catalog.rest.token-refresh-enabled="true" \
  --conf spark.sql.catalog.rest.header.X-Iceberg-Access-Delegation="vended-credentials" \
  --conf spark.sql.catalog.rest.credential=$POLARIS_CREDENTIAL
