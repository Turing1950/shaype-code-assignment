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
import java.util.UUID;

@Service
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    private final AlertConfig alertConfig;
    private final WebhookService webhookService;
    private final Counter alertsTriggeredCounter;
    private final Counter highAmountAlertsCounter;
    private final Counter watchlistAlertsCounter;
    
    public AlertService(AlertConfig alertConfig, WebhookService webhookService, MeterRegistry meterRegistry) {
        this.alertConfig = alertConfig;
        this.webhookService = webhookService;
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
    
    public void evaluateTransaction(Transaction transaction) {
        logger.debug("Evaluating transaction: {}", transaction.transactionId());
        
        boolean isHighAmount = isHighAmountAlert(transaction);
        boolean isWatchlist = isWatchlistAlert(transaction);
        
        if (isHighAmount || isWatchlist) {
            createCombinedAlert(transaction, isHighAmount, isWatchlist);
        }
    }
    
    private boolean isHighAmountAlert(Transaction transaction) {
        return transaction.amount() > alertConfig.getAmountThreshold();
    }
    
    private boolean isWatchlistAlert(Transaction transaction) {
        return alertConfig.getWatchlistAccounts().contains(transaction.fromAccount());
    }
    
    private void createCombinedAlert(Transaction transaction, boolean isHighAmount, boolean isWatchlist) {
        if (isHighAmount) {
            highAmountAlertsCounter.increment();
            alertsTriggeredCounter.increment();
            logger.warn("HIGH AMOUNT ALERT: Transaction {} amount {} exceeds threshold {}", 
                transaction.transactionId(), transaction.amount(), alertConfig.getAmountThreshold());
        }
        
        if (isWatchlist) {
            watchlistAlertsCounter.increment();
            alertsTriggeredCounter.increment();
            logger.warn("WATCHLIST ALERT: Transaction {} from watchlisted account {}", 
                transaction.transactionId(), transaction.fromAccount());
        }
        
        String reason = isHighAmount && isWatchlist ? "HIGH_AMOUNT,WATCHLIST_ACCOUNT" : 
                       isHighAmount ? "HIGH_AMOUNT" : "WATCHLIST_ACCOUNT";
        String severity = isHighAmount ? "HIGH" : "MEDIUM";
        String message = buildAlertMessage(transaction, isHighAmount, isWatchlist);
        
        Alert alert = new Alert(
            UUID.randomUUID().toString(),
            transaction.transactionId(),
            reason,
            severity,
            message,
            Instant.now(),
            transaction
        );
        
        webhookService.sendAlert(alert);
    }
    
    private String buildAlertMessage(Transaction transaction, boolean isHighAmount, boolean isWatchlist) {
        if (isHighAmount && isWatchlist) {
            return String.format("High amount transaction: %d %s exceeds threshold %d AND from watchlisted account: %s", 
                transaction.amount(), transaction.currency(), alertConfig.getAmountThreshold(), transaction.fromAccount());
        } else if (isHighAmount) {
            return String.format("High amount transaction: %d %s exceeds threshold %d", 
                transaction.amount(), transaction.currency(), alertConfig.getAmountThreshold());
        } else {
            return String.format("Transaction from watchlisted account: %s", transaction.fromAccount());
        }
    }
}