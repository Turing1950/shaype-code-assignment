package com.shaype.code.assignment.health;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final KafkaTemplate<String, ?> kafkaTemplate;

    public HealthController(KafkaTemplate<String, ?> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/health/kafka")
    public Map<String, String> kafkaHealth() {
        try {
            kafkaTemplate.getProducerFactory().createProducer().close();
            return Map.of("status", "UP", "kafka", "Available");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "kafka", "Unavailable", "error", e.getMessage());
        }
    }
}