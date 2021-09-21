FROM quay.io/rhdevelopers/ubi8-java-11
#FROM openjdk:11
RUN mkdir -p /deployments/configuration
RUN chown 1001 /deployments; chown 1001 /deployments/configuration
USER 1001
#COPY --chown=1001 configuration/dev.properties /deployments/configuration
#ES-10
COPY --chown=1001 configuration/es10-cluster.properties /deployments/configuration
COPY --chown=1001 configuration/log4j.properties /deployments/configuration
COPY --chown=1001 build/libs/kstreams-merge-standalone-0.0.1.jar /deployments
#COPY --chown=1001 configuration/truststore.p12 /deployments/configuration
#ES-10
COPY --chown=1001 configuration/es-cert.p12 /deployments/configuration
# COPY --chown=1001 configuration/es-cert.password /deployments/configuration
WORKDIR /deployments
#ENTRYPOINT [ "/deployments/run-java.sh", "configuration/dev.properties", "schema.registry.ssl.truststore.location=/deployments/configuration/truststore.p12", "schema.registry.ssl.truststore.password=mystorepassword"]
#ENTRYPOINT [ "java", "-jar /deployments/kstreams-merge-standalone-0.0.1.jar", "configuration/dev.properties"]
#ENTRYPOINT ["java", "-jar", "./kstreams-merge-standalone-0.0.1.jar", "./configuration/dev.properties"]
#ES-10
ENTRYPOINT ["java", "-Dlog4j.configuration=file:/deployments/configuration/log4j.properties", "-Djavax.net.ssl.trustStoreType=pkcs12", "-Djavax.net.ssl.trustStore=/deployments/configuration/es-cert.p12", "-Djavax.net.ssl.trustStorePassword=xmNQ0GbZTByB", "-jar", "./kstreams-merge-standalone-0.0.1.jar", "./configuration/es10-cluster.properties"]
