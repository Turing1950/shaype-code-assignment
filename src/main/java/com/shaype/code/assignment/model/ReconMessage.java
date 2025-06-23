package com.shaype.code.assignment.model;

import java.time.Instant;

public record ReconMessage(
    String contextId,
    boolean wasAlerted,
    Instant timestamp,
    String outcome
) {}