package com.shaype.code.assignment.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record Transaction(
    @JsonProperty("transaction_id") String transactionId,
    Long amount,
    String currency,
    @JsonProperty("from_account") String fromAccount,
    @JsonProperty("to_account") String toAccount,
    Instant timestamp,
    @JsonProperty("context_id") String contextId
) {}