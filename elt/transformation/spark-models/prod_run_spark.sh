# Run spark like in production environment using polaris catalog

source $HOME/Documents/setup/python_env/spark_env/bin/activate

source aws_creds.sh

export JOB_NAME=polarissmoke
export CATALOG_NAME=dev_catalog
export CC_POLARIS_CREDENTIAL=$(cat  ../../../../kubernetes/polaris/client_credentials) # Mandatory

export APP_JAR=target/transformation-test-0.1.0.jar

export COMET_JAR=../../../ddl/spark_jars/comet-spark-spark4.1_2.13-1.0.0.jar
export JAR_PATH=$COMET_JAR,../../../ddl/spark_jars/iceberg-aws-bundle-1.11.0.jar,../../../ddl/spark_jars/iceberg-spark-runtime-4.1_2.13-1.11.0.jar,../../../ddl/spark_jars/hadoop-aws-3.5.0.jar,../../../ddl/spark_jars/s3-transfer-manager-2.47.4.jar

spark-submit \
  --jars $JAR_PATH \
  --master local[*] \
  --conf spark.app.name=$JOB_NAME \
  --conf spark.driver.host=127.0.0.1 \
  --conf spark.driver.bindAddress=127.0.0.1 \
  --conf spark.local.ip=127.0.0.1 \
  --conf spark.ui.host=127.0.0.1 \
  --conf spark.ui.bindAddress=127.0.0.1 \
  --conf spark.hadoop.fs.s3a.aws.credentials.provider=software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider \
  --conf spark.sql.extensions="org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions" \
  --conf spark.sql.defaultCatalog="rest" \
  --conf spark.sql.catalog.rest="org.apache.iceberg.spark.SparkCatalog" \
  --conf spark.sql.catalog.rest.type="rest" \
  --conf spark.sql.catalog.rest.uri="http://localhost:8181/api/catalog" \
  --conf spark.sql.catalog.rest.warehouse=$CATALOG_NAME \
  --conf spark.sql.catalog.rest.auth.type="oauth2" \
  --conf spark.sql.catalog.rest.oauth2-server-uri="http://localhost:8181/api/catalog/v1/oauth/tokens" \
  --conf spark.sql.catalog.rest.scope="PRINCIPAL_ROLE:ALL" \
  --conf spark.sql.catalog.rest.credential=$ETL_POLARIS_CREDENTIAL \
  --conf spark.sql.catalog.rest.token-refresh-enabled="true" \
  --conf spark.sql.catalog.rest.header.X-Iceberg-Access-Delegation="vended-credentials" \
  --conf spark.yarn.maxAppAttempts=1 \
  --conf spark.driver.extraClassPath=$COMET_JAR \
  --conf spark.executor.extraClassPath=$COMET_JAR \
  --conf spark.plugins=org.apache.spark.CometPlugin \
  --conf spark.shuffle.manager=org.apache.spark.sql.comet.execution.shuffle.CometShuffleManager \
  --conf spark.comet.explain.fallback.enabled=true \
  $APP_JAR \
    --select="model:StgMeteoFranceObservationModel" \
    --modelregistry.path=./prod_model_registry.yaml \
    --airflow.xcompath="./return.json"