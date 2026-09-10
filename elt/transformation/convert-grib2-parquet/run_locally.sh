#source aws_creds.sh

java -jar target/app.jar \
  --etl.jobid=CONV_GRIB \
  --etl.jobrunid=abcd \
  --etl.sunkfiles="$(cat extraction_result.json)" \
  --etl.outputdir=./output/ \
  --etl.airflow.xcompath=./return.json