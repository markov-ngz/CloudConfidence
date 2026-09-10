source $HOME/Documents/setup/python_env/spark_env/bin/activate

#source aws_creds.sh
export CATALOG_NAME=dev_catalog
export APP_JAR=target/spark-model-0.1.0.jar

export CC_POLARIS_CREDENTIAL="abc" # Mandatory

export COMET_JAR=../../../ddl/spark_jars/comet-spark-spark4.1_2.13-1.0.0.jar
export JAR_PATH=$COMET_JAR,../../../ddl/spark_jars/iceberg-aws-bundle-1.11.0.jar,../../../ddl/spark_jars/iceberg-spark-runtime-4.1_2.13-1.11.0.jar,../../../ddl/spark_jars/hadoop-aws-3.5.0.jar,../../../ddl/spark_jars/s3-transfer-manager-2.47.4.jar

export JOB_NAME=localrun

spark-submit \
  --jars $JAR_PATH \
  --master local[*] \
  --conf spark.app.name=$JOB_NAME \
  --conf spark.driver.host=127.0.0.1 \
  --conf spark.driver.bindAddress=127.0.0.1 \
  --conf spark.local.ip=127.0.0.1 \
  --conf spark.ui.host=127.0.0.1 \
  --conf spark.ui.bindAddress=127.0.0.1 \
  --conf spark.driver.extraClassPath=$COMET_JAR \
  --conf spark.executor.extraClassPath=$COMET_JAR \
  --conf spark.plugins=org.apache.spark.CometPlugin \
  --conf spark.shuffle.manager=org.apache.spark.sql.comet.execution.shuffle.CometShuffleManager \
  --conf spark.comet.explain.fallback.enabled=true \
  $APP_JAR \
  --polaris=abc \
  --select="model:MartForecastErrorOverTimeModel" \
  --modelregistry.path=./model_registry.yaml \
  --airflow.xcompath=./return.json