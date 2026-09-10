source aws_creds.sh

export ETL_SOURCE_APPLICATIONID=$(cat $HOME/meteofrance-api/application_id)
export ETL_SOURCE_DEPARTMENTIDS=988
export ETL_SOURCE_FORMAT=csv

export ETL_SINK_BUCKET=cloudconfidence414-landing-zone
export ETL_SINK_BASEDIRECTORY=extraction/meteofrance-observation
