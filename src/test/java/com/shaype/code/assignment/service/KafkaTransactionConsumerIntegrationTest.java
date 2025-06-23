package com.shaype.code.assignment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shaype.code.assignment.model.Transaction;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.MeterRegistry;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"transactions"})
@ActiveProfiles("test")
class KafkaTransactionConsumerIntegrationTest {

    @Value("${kafka.topic.transactions}")
    private String topic;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private CountDownLatch latch;
    
    @Autowired
    private MeterRegistry meterRegistry;

    private Producer<String, String> producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producer = new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new StringSerializer()).createProducer();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    void shouldConsumeKafkaMessage() throws JsonProcessingException, InterruptedException {
        // Given
        Transaction transaction = new Transaction(
            "abc123", 120000L, "USD", "12345", "67890", 
            Instant.parse("2025-06-23T10:00:00Z"), null
        );
        String json = objectMapper.writeValueAsString(transaction);

        // When
        producer.send(new ProducerRecord<>(topic, "key", json));
        producer.flush();

        // Then
        boolean messageConsumed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(messageConsumed, "Message was not consumed within timeout");
        
        // Verify metrics
        double processedCount = meterRegistry.get("kafka.transactions.processed").counter().count();
        double generatedContextIdCount = meterRegistry.get("kafka.transactions.generated_context_id").counter().count();
        
        assertEquals(1, processedCount, "Should have processed 1 transaction");
        assertEquals(1, generatedContextIdCount, "Should have generated 1 context ID");
    }
}