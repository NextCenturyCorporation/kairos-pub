package com.nextcentury.kairos.result.enqueue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.util.UUID;

public class OutputTopicPoster {

    public static String CLIENT_ID = "client1";
    private Properties producerProps = new Properties();

    private static final Logger logger = LogManager.getLogger(OutputTopicPoster.class);

    private String kafkaBrokers = null;
    private String outputTopic = null;
    private String fqKey = null;
    private String submissionNotificationMessage = null;

    private void initKafkaConfig() {
        logger.info("Kafka brokers - " + kafkaBrokers);
        logger.info("Output Topic - " + outputTopic);
        logger.info("notification message - " + submissionNotificationMessage);
        logger.info("");

        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokers);
        producerProps.put(ProducerConfig.CLIENT_ID_CONFIG, CLIENT_ID);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    }


    public void postSubmission(String kafkaBrokers, String outputTopic, String fqKey, String submissionNotificationMessage) {
        this.submissionNotificationMessage = submissionNotificationMessage;
        this.outputTopic = outputTopic;
        this.kafkaBrokers = kafkaBrokers;

        initKafkaConfig();
        post();
    }

    private void post() {
        KafkaProducer producer = new KafkaProducer<String, String>(producerProps);
        try {
            // while (true) {
            try {
                String recordKey = UUID.randomUUID().toString();
                ProducerRecord<String, String> record = new ProducerRecord<String, String>(outputTopic, recordKey,
                        submissionNotificationMessage);

                RecordMetadata metadata = (RecordMetadata) producer.send(record).get();
                logger.info(new StringBuilder("Published kairos submission notification - ").append(recordKey)
                        .append(" Kafka topic - \'").append(outputTopic).append("\', partition ")
                        .append(metadata.partition()).append(" with offset ").append(metadata.offset()).toString());
                logger.info(submissionNotificationMessage);

                // sleep for 5 seconds before publishing next topic
                // Thread.sleep(5000);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            // }
        } finally {
            if (producer != null) {
                producer.close();
            }
        }
    }
}
