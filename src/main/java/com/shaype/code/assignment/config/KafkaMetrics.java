package com.shaype.code.assignment.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.atomic.AtomicInteger;

public class KafkaMetrics {
    private final Counter transactionsProcessed;
    private final Counter transactionsWithGeneratedContextId;
    private final Timer processingTime;
    private final AtomicInteger activeTransactions;

    public KafkaMetrics(MeterRegistry registry) {
        this.transactionsProcessed = Counter.builder("kafka.transactions.processed")
                .description("Number of transactions processed")
                .register(registry);
        
        this.transactionsWithGeneratedContextId = Counter.builder("kafka.transactions.generated_context_id")
                .description("Number of transactions with generated context ID")
                .register(registry);
        
        this.processingTime = Timer.builder("kafka.transactions.processing_time")
                .description("Time taken to process transactions")
                .register(registry);
                
        this.activeTransactions = registry.gauge("kafka.transactions.active", 
                new AtomicInteger(0));
    }

    public Counter getTransactionsProcessed() {
        return transactionsProcessed;
    }

    public Counter getTransactionsWithGeneratedContextId() {
        return transactionsWithGeneratedContextId;
    }

    public Timer getProcessingTime() {
        return processingTime;
    }
    
    public AtomicInteger getActiveTransactions() {
        return activeTransactions;
    }
}