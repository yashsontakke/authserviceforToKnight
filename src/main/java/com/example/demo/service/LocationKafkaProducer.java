package com.example.demo.service;


import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.LocationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.kafka.support.SendResult;



@Service
public class LocationKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(LocationKafkaProducer.class);

    private final KafkaTemplate<String, LocationMessage> kafkaTemplate;
    private final String topicName;

     // Inject the specific KafkaTemplate and topic name
    public LocationKafkaProducer(KafkaTemplate<String, LocationMessage> kafkaTemplate,
                                 @Value("${app.kafka.topic.location-updates}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    /**
     * Sends the LocationMessage to the configured Kafka topic.
     * Uses userId as the Kafka message key for partitioning.
     * @param message The LocationMessage to send.
     */
    public void sendMessage(LocationMessage message) {
        if (message == null || message.getUserId() == null) {
            log.warn("Attempted to send null message or message with null userId to Kafka topic '{}'.", topicName);
            return;
        }

     
        // Send asynchronously. The CompletableFuture allows adding callbacks.
        CompletableFuture<SendResult<String, LocationMessage>> future =
                kafkaTemplate.send(topicName, message.getUserId(), message);

        // Add a non-blocking callback to log success or failure
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // Success
                log.info("Sent location message for user '{}' to topic '{}' partition=[{}] with offset=[{}]",
                        message.getUserId(), topicName, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } else {
                // Failure
                log.error("Unable to send location message for user '{}' to topic '{}' due to: {}",
                        message.getUserId(), topicName, ex.getMessage());
                // Consider adding metrics or specific error handling here
            }
        });
    }
}