package com.shaype.code.assignment.service;

import com.shaype.code.assignment.config.KafkaMetrics;
import com.shaype.code.assignment.model.Transaction;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerServiceTest {

    @Mock
    private KafkaMetrics metrics;
    
    @Mock
    private Counter processedCounter;
    
    @Mock
    private Counter generatedContextIdCounter;
    
    private TransactionConsumerService consumerService;
    
    @BeforeEach
    void setUp() {
        Mockito.when(metrics.getTransactionsProcessed()).thenReturn(processedCounter);
        Mockito.when(metrics.getTransactionsWithGeneratedContextId()).thenReturn(generatedContextIdCounter);
        
        consumerService = new TransactionConsumerService(metrics);
    }

    @Test
    void shouldConsumeTransactionWithContextId() {
        Transaction transaction = new Transaction(
            "abc123", 120000L, "USD", "12345", "67890", 
            Instant.parse("2025-06-23T10:00:00Z"), "existing-context"
        );

        assertDoesNotThrow(() -> consumerService.consume(transaction));
    }

    @Test
    void shouldConsumeTransactionWithoutContextId() {
        Transaction transaction = new Transaction(
            "abc123", 120000L, "USD", "12345", "67890", 
            Instant.parse("2025-06-23T10:00:00Z"), null
        );

        assertDoesNotThrow(() -> consumerService.consume(transaction));
    }
}