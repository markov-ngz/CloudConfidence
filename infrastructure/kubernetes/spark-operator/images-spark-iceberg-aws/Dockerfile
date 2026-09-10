FROM apache/spark:4.1.3

COPY jars/hadoop-aws-3.5.0.jar /opt/spark/jars/hadoop-aws-3.5.0.jar
COPY jars/bundle-2.47.4.jar /opt/spark/jars/bundle-2.47.4.jar
COPY jars/analyticsaccelerator-s3-1.3.1.jar /opt/spark/jars/analyticsaccelerator-s3-1.3.1.jar
COPY jars/celeborn-client-spark-4-shaded_2.13-0.7.0.jar /opt/spark/jars/celeborn-client-spark-4-shaded_2.13-0.7.0.jar


ENV SPARK_EXTRA_CLASSPATH=/opt/spark/extra-jars/*
COPY extra-jars /opt/spark/extra-jars/

# Switch to user root so we can add additional jars and configuration files.
USER root

# Setup for the Prometheus JMX exporter.
ENV JMX_EXPORTER_AGENT_VERSION=1.1.0
ADD https://github.com/prometheus/jmx_exporter/releases/download/${JMX_EXPORTER_AGENT_VERSION}/jmx_prometheus_javaagent-${JMX_EXPORTER_AGENT_VERSION}.jar /opt/spark/jars
RUN chmod 644 /opt/spark/jars/jmx_prometheus_javaagent-${JMX_EXPORTER_AGENT_VERSION}.jar

USER 185