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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = "server.port=8080")
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"transactions"})
@ActiveProfiles("test")
class IntegrationTest {
    


    @Value("${kafka.topic.transactions}")
    private String topic;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private CountDownLatch latch;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @LocalServerPort
    private int port;
    


    private Producer<String, String> producer;
    private ObjectMapper objectMapper;
    private ListAppender<ILoggingEvent> logAppender;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("alert.webhook.url", () -> "http://localhost:8080/test-webhook");
    }

    @BeforeEach
    void setUp() {
        Map<String, Object> producerProps = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        producer = new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new StringSerializer()).createProducer();
        
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Setup log appender
        Logger logger = (Logger) LoggerFactory.getLogger("com.shaype.code.assignment.service");
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        if (producer != null) {
            producer.close();
        }
    }

    @Test
    void shouldNotTriggerAlertForNormalTransaction() throws JsonProcessingException, InterruptedException {
        double initialAlerts = meterRegistry.get("alerts.triggered.total").counter().count();
        
        Transaction transaction = new Transaction(
            "tx-1", 5000L, "USD", "normal-account", "dest-account", 
            Instant.parse("2025-06-23T10:00:00Z"), null
        );
        
        producer.send(new ProducerRecord<>(topic, "key", objectMapper.writeValueAsString(transaction)));
        producer.flush();
        
        Thread.sleep(2000);
        
        double alertsTriggered = meterRegistry.get("alerts.triggered.total").counter().count() - initialAlerts;
        assertEquals(0, alertsTriggered, "Should not trigger any alerts");
    }
    
    @Test
    void shouldTriggerHttpAlertForHighAmountTransaction() throws JsonProcessingException, InterruptedException {
        double initialAlerts = meterRegistry.get("alerts.triggered.total").counter().count();
        
        Transaction transaction = new Transaction(
            "tx-2", 120000L, "USD", "normal-account", "dest-account", 
            Instant.parse("2025-06-23T10:00:00Z"), null
        );
        
        TestWebhookController.webhookLatch = new CountDownLatch(1);
        producer.send(new ProducerRecord<>(topic, "key", objectMapper.writeValueAsString(transaction)));
        producer.flush();
        
        assertTrue(TestWebhookController.webhookLatch.await(10, TimeUnit.SECONDS), "Webhook not received");
        
        assertEquals("HIGH_AMOUNT", TestWebhookController.receivedWebhook.get("reason"));
        assertEquals("HIGH", TestWebhookController.receivedWebhook.get("severity"));
        
        double alertsTriggered = meterRegistry.get("alerts.triggered.total").counter().count() - initialAlerts;
        assertEquals(1, alertsTriggered, "Should have triggered 1 alert");
        
        // Verify trace ID in logs
        boolean hasTraceId = logAppender.list.stream()
            .anyMatch(event -> event.getMDCPropertyMap().containsKey("traceId"));
        assertTrue(hasTraceId, "Should have trace ID in logs");
    }
    
    @Test
    void shouldTriggerHttpAlertForWatchlistAccount() throws JsonProcessingException, InterruptedException {
        double initialAlerts = meterRegistry.get("alerts.triggered.total").counter().count();
        
        Transaction transaction = new Transaction(
            "tx-3", 5000L, "USD", "suspicious-account-1", "dest-account", 
            Instant.parse("2025-06-23T10:00:00Z"), null
        );
        
        TestWebhookController.webhookLatch = new CountDownLatch(1);
        producer.send(new ProducerRecord<>(topic, "key", objectMapper.writeValueAsString(transaction)));
        producer.flush();
        
        assertTrue(TestWebhookController.webhookLatch.await(10, TimeUnit.SECONDS), "Webhook not received");
        
        assertEquals("WATCHLIST_ACCOUNT", TestWebhookController.receivedWebhook.get("reason"));
        assertEquals("MEDIUM", TestWebhookController.receivedWebhook.get("severity"));
        
        double alertsTriggered = meterRegistry.get("alerts.triggered.total").counter().count() - initialAlerts;
        assertEquals(1, alertsTriggered, "Should have triggered 1 alert");
    }
    
    @Test
    void shouldTriggerMultipleAlertsForWatchlistAndHighAmount() throws JsonProcessingException, InterruptedException {
        double initialAlertsTriggered = meterRegistry.get("alerts.triggered.total").counter().count();
        double initialHighAmountAlerts = meterRegistry.get("alerts.high_amount.total").counter().count();
        double initialWatchlistAlerts = meterRegistry.get("alerts.watchlist.total").counter().count();
        
        Transaction transaction = new Transaction(
            "tx-4", 150000L, "USD", "suspicious-account-2", "dest-account", 
            Instant.parse("2025-06-23T10:00:00Z"), null
        );
        
        TestWebhookController.webhookLatch = new CountDownLatch(1);
        producer.send(new ProducerRecord<>(topic, "key", objectMapper.writeValueAsString(transaction)));
        producer.flush();
        
        assertTrue(TestWebhookController.webhookLatch.await(10, TimeUnit.SECONDS), "Webhook not received");
        
        assertEquals("HIGH_AMOUNT,WATCHLIST_ACCOUNT", TestWebhookController.receivedWebhook.get("reason"));
        assertEquals("HIGH", TestWebhookController.receivedWebhook.get("severity"));
        
        Thread.sleep(1000);
        
        double alertsTriggered = meterRegistry.get("alerts.triggered.total").counter().count() - initialAlertsTriggered;
        double highAmountAlerts = meterRegistry.get("alerts.high_amount.total").counter().count() - initialHighAmountAlerts;
        double watchlistAlerts = meterRegistry.get("alerts.watchlist.total").counter().count() - initialWatchlistAlerts;
        
        assertEquals(2, alertsTriggered, "Should have triggered 2 alerts");
        assertEquals(1, highAmountAlerts, "Should have 1 high amount alert");
        assertEquals(1, watchlistAlerts, "Should have 1 watchlist alert");
    }
}