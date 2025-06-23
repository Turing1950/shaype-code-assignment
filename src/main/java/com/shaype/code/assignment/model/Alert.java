package com.shaype.code.assignment.model;

import java.time.Instant;

public record Alert(
    String alertId,
    String transactionId,
    String ruleTriggered,
    String severity,
    String message,
    Instant timestamp,
    Transaction transaction
) {}