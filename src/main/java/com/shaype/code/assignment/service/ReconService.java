package com.shaype.code.assignment.service;

import com.shaype.code.assignment.model.ReconMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ReconService {
    
    private static final Logger logger = LoggerFactory.getLogger(ReconService.class);
    
    private final KafkaTemplate<String, ReconMessage> kafkaTemplate;
    private final String reconTopic;
    
    public ReconService(KafkaTemplate<String, ReconMessage> reconKafkaTemplate,
                       @Value("${kafka.topic.recon}") String reconTopic) {
        this.kafkaTemplate = reconKafkaTemplate;
        this.reconTopic = reconTopic;
    }
    
    public void sendReconMessage(String contextId, boolean wasAlerted, String outcome) {
        ReconMessage reconMessage = new ReconMessage(
            contextId,
            wasAlerted,
            Instant.now(),
            outcome
        );
        
        kafkaTemplate.send(reconTopic, contextId, reconMessage);
        logger.debug("Sent recon message for context {}: wasAlerted={}, outcome={}", 
                    contextId, wasAlerted, outcome);
    }
}