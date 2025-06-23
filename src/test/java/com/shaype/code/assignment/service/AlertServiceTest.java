package com.shaype.code.assignment.service;

import com.shaype.code.assignment.config.AlertConfig;
import com.shaype.code.assignment.model.Alert;
import com.shaype.code.assignment.model.Transaction;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AlertServiceTest {

    private AlertService alertService;
    private AlertConfig alertConfig;

    @BeforeEach
    void setUp() {
        alertConfig = new AlertConfig();
        alertConfig.setAmountThreshold(10000L);
        alertConfig.setWatchlistAccounts(Set.of("suspicious-account"));
        
        alertService = new AlertService(alertConfig, new SimpleMeterRegistry());
    }

    @Test
    void shouldTriggerHighAmountAlert() {
        Transaction transaction = new Transaction(
            "tx-1", 15000L, "USD", "account-1", "account-2", Instant.now(), "ctx-1"
        );

        Optional<Alert> alert = alertService.evaluateTransaction(transaction);

        assertTrue(alert.isPresent());
        assertEquals("HIGH_AMOUNT", alert.get().ruleTriggered());
        assertEquals("HIGH", alert.get().severity());
    }

    @Test
    void shouldTriggerWatchlistAlert() {
        Transaction transaction = new Transaction(
            "tx-2", 5000L, "USD", "suspicious-account", "account-2", Instant.now(), "ctx-2"
        );

        Optional<Alert> alert = alertService.evaluateTransaction(transaction);

        assertTrue(alert.isPresent());
        assertEquals("WATCHLIST_ACCOUNT", alert.get().ruleTriggered());
        assertEquals("MEDIUM", alert.get().severity());
    }

    @Test
    void shouldNotTriggerAlert() {
        Transaction transaction = new Transaction(
            "tx-3", 5000L, "USD", "normal-account", "account-2", Instant.now(), "ctx-3"
        );

        Optional<Alert> alert = alertService.evaluateTransaction(transaction);

        assertFalse(alert.isPresent());
    }
}