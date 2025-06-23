package com.shaype.code.assignment.service;

import com.shaype.code.assignment.config.KafkaMetrics;
import com.shaype.code.assignment.model.Transaction;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionConsumerService.class);
    
    private final KafkaMetrics metrics;
    private final AlertService alertService;
    private final ReconService reconService;
    
    @Autowired
    public TransactionConsumerService(KafkaMetrics metrics, AlertService alertService, ReconService reconService) {
        this.metrics = metrics;
        this.alertService = alertService;
        this.reconService = reconService;
    }

    @KafkaListener(topics = "${kafka.topic.transactions}")
    public void consume(Transaction transaction) {
        if (metrics != null) {
            metrics.getActiveTransactions().incrementAndGet();
        }
        Timer.Sample sample = Timer.start();
        
        try {
            Transaction processedTransaction = ensureContextId(transaction);
            MDC.put("traceId", processedTransaction.contextId());
            
            logger.info("Received transaction: {}", processedTransaction);
            
            // Process transaction here
            processTransaction(processedTransaction);
            
            // Increment processed counter
            if (metrics != null) {
                metrics.getTransactionsProcessed().increment();
            }
        } finally {
            MDC.clear();
            if (metrics != null) {
                sample.stop(metrics.getProcessingTime());
                metrics.getActiveTransactions().decrementAndGet();
            }
        }
    }

    private Transaction ensureContextId(Transaction transaction) {
        if (transaction.contextId() == null || transaction.contextId().isEmpty()) {
            if (metrics != null) {
                metrics.getTransactionsWithGeneratedContextId().increment();
            }
            return new Transaction(
                transaction.transactionId(),
                transaction.amount(),
                transaction.currency(),
                transaction.fromAccount(),
                transaction.toAccount(),
                transaction.timestamp(),
                UUID.randomUUID().toString()
            );
        }
        return transaction;
    }

    private void processTransaction(Transaction transaction) {
        logger.info("Processing transaction {} with context {}",
                   transaction.transactionId(), transaction.contextId());
        
        // Evaluate transaction for alerts
        boolean wasAlerted = hasAlerts(transaction);
        alertService.evaluateTransaction(transaction);
        
        // Send recon message
        String outcome = wasAlerted ? "ALERTED" : "PROCESSED";
        reconService.sendReconMessage(transaction.contextId(), wasAlerted, outcome);
    }
    
    private boolean hasAlerts(Transaction transaction) {
        return transaction.amount() > 10000 || 
               (transaction.fromAccount().equals("suspicious-account-1") || 
                transaction.fromAccount().equals("suspicious-account-2"));
    }
    

}