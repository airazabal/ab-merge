package io.confluent.developer;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.config.SaslConfigs;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import io.ab.developer.avro.pricing;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import org.apache.kafka.streams.kstream.Produced;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import java.util.Collections;


public class MergeStreams {
    public static final String SCHEMA_REGISTRY_SSL_TRUSTSTORE_LOCATION = "schema.registry.ssl.truststore.location"; 
    public static final String SCHEMA_REGISTRY_SSL_TRUSTSTORE_PASSWORD = "schema.registry.ssl.truststore.password"; 
    public static final String SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO = "basic.auth.user.info";


    public Topology buildTopology(Properties allProps) {
        final StreamsBuilder builder = new StreamsBuilder();

        final String pricingTopic = allProps.getProperty("input.pricing.topic.name");
        final String pricingDeleteTopic = allProps.getProperty("input.pricingDelete.topic.name");
        final String allPricingTopic = allProps.getProperty("output.topic.name");
        // add specific schemas
        final Serde<String> stringSerde = Serdes.String();
        //final Serde<pricing> specificAvroSerde = new SpecificAvroSerde<>();
        final Serde<GenericRecord> genericAvroSerde = new GenericAvroSerde();
        final boolean isKeySerde = false;
        genericAvroSerde.configure(
            Collections.singletonMap(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, allProps.getProperty("schema.registry.url")),
            isKeySerde);
        final KStream<String, GenericRecord> pricingMessage = builder.stream(pricingTopic);
        final KStream<String, GenericRecord> pricingDeleteMessage = builder.stream(pricingDeleteTopic);
        final KStream<String, GenericRecord> allPricingMessage = pricingMessage.merge(pricingDeleteMessage);
        final KafkaStreams streams = new KafkaStreams(builder.build(), allProps);

// end of avro schemas
       
        allPricingMessage.to(allPricingTopic,Produced.with(stringSerde, genericAvroSerde));
        return builder.build();
    }

    public Properties loadEnvProperties(String fileName) throws IOException {
        Properties allProps = new Properties();
        FileInputStream input = new FileInputStream(fileName);
        allProps.load(input);
        input.close();

        return allProps;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException("This program takes one argument: the path to an environment configuration file.");
        }

        MergeStreams ms = new MergeStreams();
        Properties allProps = ms.loadEnvProperties(args[0]);
        allProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        allProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
        allProps.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, allProps.getProperty("schema.registry.url"));
        allProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, allProps.getProperty("security.protocol.config"));
        allProps.put(SaslConfigs.SASL_MECHANISM, allProps.getProperty("sasl.mechanism.config"));
        allProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG,allProps.getProperty("ssl.truststore.location.config"));
        allProps.put(SaslConfigs.SASL_JAAS_CONFIG,allProps.getProperty("sasl.jaas.config"));
        allProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, allProps.getProperty("ssl.truststore.password.config"));
        allProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, allProps.getProperty("ssl.keystore.location.config"));
        allProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, allProps.getProperty("ssl.keystore.password.config"));
        //allProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, allProps.getProperty("ssl.key.location.config"));

        allProps.put(SCHEMA_REGISTRY_SSL_TRUSTSTORE_LOCATION, allProps.getProperty("schema.registry.ssl.truststore.location"));
        allProps.put(SCHEMA_REGISTRY_SSL_TRUSTSTORE_PASSWORD, allProps.getProperty("schema.registry.ssl.truststore.password"));
        // for RBAC
        allProps.put(AbstractKafkaSchemaSerDeConfig.BASIC_AUTH_CREDENTIALS_SOURCE, "USER_INFO");
        allProps.put(SCHEMA_REGISTRY_BASIC_AUTH_USER_INFO, allProps.getProperty("basic.auth.user.info"));
        



        Topology topology = ms.buildTopology(allProps);

        
        final KafkaStreams streams = new KafkaStreams(topology, allProps);
        final CountDownLatch latch = new CountDownLatch(1);

        // Attach shutdown handler to catch Control-C.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close(Duration.ofSeconds(5));
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
