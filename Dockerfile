#FROM quay.io/rhdevelopers/ubi8-java-11
FROM openjdk:11
RUN mkdir -p /deployments/configuration
RUN chown 1001 /deployments; chown 1001 /deployments/configuration
USER 1001
COPY --chown=1001 configuration/dev.properties /deployments/configuration
COPY --chown=1001 build/libs/kstreams-merge-standalone-0.0.1.jar /deployments
WORKDIR /deployments
#ENTRYPOINT [ "/deployments/run-java.sh", "configuration/dev.properties", "schema.registry.ssl.truststore.location=/deployments/configuration/truststore.p12", "schema.registry.ssl.truststore.password=mystorepassword"]
#ENTRYPOINT [ "java", "-jar /deployments/kstreams-merge-standalone-0.0.1.jar", "configuration/dev.properties"]
ENTRYPOINT ["java", "-Djavax.net.ssl.trustStore=/deployments/configuration/truststore.p12", "-Djavax.net.ssl.trustStorePassword=mystorepassword", "-jar", "./kstreams-merge-standalone-0.0.1.jar", "./configuration/dev.properties"]
