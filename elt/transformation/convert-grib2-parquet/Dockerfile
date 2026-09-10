############################
# Stage 1: runtime image ( skip custom jre as application jar is too heavy )
############################
FROM eclipse-temurin:21-jdk-jammy AS runtime

RUN adduser --shell /bin/bash --uid 1000 appuser

WORKDIR /app

COPY --chown=appuser:appuser target/app.jar /app/app.jar

RUN chown appuser:appuser /app/app.jar

USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]