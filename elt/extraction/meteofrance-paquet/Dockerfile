############################
# Stage 1: build the custom JRE
############################
FROM eclipse-temurin:21-jdk-jammy AS builder

ARG JAR_FILE=target/app.jar
WORKDIR /app

COPY ${JAR_FILE} app.jar

RUN mkdir -p unpacked && cd unpacked && jar -xf /app/app.jar

RUN jdeps --ignore-missing-deps \
      -q \
      --recursive \
      --multi-release 21 \
      --print-module-deps \
      --class-path="unpacked/BOOT-INF/lib/*" \
      app.jar > dependencies.info && \
    test -s dependencies.info

RUN jlink --verbose \
      --compress 2 \
      --strip-java-debug-attributes \
      --no-header-files \
      --no-man-pages \
      --output /jre \
      --add-modules $(cat dependencies.info),jdk.crypto.ec,jdk.crypto.cryptoki,java.security.jgss

############################
# Stage 2: runtime image
############################

# Using debian as alpine as Azure uses netty-tcnative-boringssl-static.
# It ships native .so libraries (JNI bindings to BoringSSL) that are compiled against glibc.
# However Alpine Linux uses musl libc not glibc ( stabdard GNU C library )
FROM debian:13-slim AS runtime

RUN useradd --create-home --shell /bin/bash appuser
WORKDIR /home/appuser

ENV JAVA_HOME=/jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=builder --chown=appuser:appuser /jre $JAVA_HOME
COPY --from=builder --chown=appuser:appuser /app/app.jar /app/app.jar

# Debug: Check if /jre/bin/java exists in the runtime
RUN ls -la /jre/bin/java

USER appuser

ENTRYPOINT ["/jre/bin/java", "-jar", "/app/app.jar"]