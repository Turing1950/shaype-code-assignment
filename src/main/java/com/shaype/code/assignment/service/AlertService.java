package com.shaype.code.assignment.service;

import com.shaype.code.assignment.config.AlertConfig;
import com.shaype.code.assignment.model.Alert;
import com.shaype.code.assignment.model.Transaction;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    private final AlertConfig alertConfig;
    private final Counter alertsTriggeredCounter;
    private final Counter highAmountAlertsCounter;
    private final Counter watchlistAlertsCounter;
    
    public AlertService(AlertConfig alertConfig, MeterRegistry meterRegistry) {
        this.alertConfig = alertConfig;
        this.alertsTriggeredCounter = Counter.builder("alerts.triggered.total")
                .description("Total number of alerts triggered")
                .register(meterRegistry);
        this.highAmountAlertsCounter = Counter.builder("alerts.high_amount.total")
                .description("Total number of high amount alerts")
                .register(meterRegistry);
        this.watchlistAlertsCounter = Counter.builder("alerts.watchlist.total")
                .description("Total number of watchlist alerts")
                .register(meterRegistry);
    }
    
    public Optional<Alert> evaluateTransaction(Transaction transaction) {
        logger.debug("Evaluating transaction: {}", transaction.transactionId());
        
        if (isHighAmountAlert(transaction)) {
            return Optional.of(createHighAmountAlert(transaction));
        }
        
        if (isWatchlistAlert(transaction)) {
            return Optional.of(createWatchlistAlert(transaction));
        }
        
        return Optional.empty();
    }
    
    private boolean isHighAmountAlert(Transaction transaction) {
        return transaction.amount() > alertConfig.getAmountThreshold();
    }
    
    private boolean isWatchlistAlert(Transaction transaction) {
        return alertConfig.getWatchlistAccounts().contains(transaction.fromAccount());
    }
    
    private Alert createHighAmountAlert(Transaction transaction) {
        highAmountAlertsCounter.increment();
        alertsTriggeredCounter.increment();
        
        Alert alert = new Alert(
            UUID.randomUUID().toString(),
            transaction.transactionId(),
            "HIGH_AMOUNT",
            "HIGH",
            String.format("High amount transaction: %d %s exceeds threshold %d", 
                transaction.amount(), transaction.currency(), alertConfig.getAmountThreshold()),
            Instant.now(),
            transaction
        );
        
        logger.warn("HIGH AMOUNT ALERT: Transaction {} amount {} exceeds threshold {}", 
            transaction.transactionId(), transaction.amount(), alertConfig.getAmountThreshold());
        
        return alert;
    }
    
    private Alert createWatchlistAlert(Transaction transaction) {
        watchlistAlertsCounter.increment();
        alertsTriggeredCounter.increment();
        
        Alert alert = new Alert(
            UUID.randomUUID().toString(),
            transaction.transactionId(),
            "WATCHLIST_ACCOUNT",
            "MEDIUM",
            String.format("Transaction from watchlisted account: %s", transaction.fromAccount()),
            Instant.now(),
            transaction
        );
        
        logger.warn("WATCHLIST ALERT: Transaction {} from watchlisted account {}", 
            transaction.transactionId(), transaction.fromAccount());
        
        return alert;
    }
}