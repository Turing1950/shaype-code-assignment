package com.shaype.code.assignment.service;

import com.shaype.code.assignment.config.AlertConfig;
import com.shaype.code.assignment.model.Alert;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Service
public class WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    private final RestTemplate restTemplate;
    private final AlertConfig alertConfig;
    private final Counter webhookSuccessCounter;
    private final Counter webhookFailureCounter;
    private final Timer webhookTimer;
    
    public WebhookService(RestTemplateBuilder restTemplateBuilder, AlertConfig alertConfig, MeterRegistry meterRegistry) {
        this.alertConfig = alertConfig;
        this.restTemplate = restTemplateBuilder
            .connectTimeout(Duration.ofMillis(alertConfig.getWebhook().getTimeoutMs()))
            .readTimeout(Duration.ofMillis(alertConfig.getWebhook().getTimeoutMs()))
            .build();
        this.webhookSuccessCounter = Counter.builder("webhook.alerts.success.total")
            .description("Total successful webhook alert deliveries")
            .register(meterRegistry);
        this.webhookFailureCounter = Counter.builder("webhook.alerts.failure.total")
            .description("Total failed webhook alert deliveries")
            .register(meterRegistry);
        this.webhookTimer = Timer.builder("webhook.alerts.duration")
            .description("Webhook alert delivery duration")
            .register(meterRegistry);
    }
    
    public void sendAlert(Alert alert) {
        String webhookUrl = alertConfig.getWebhook().getUrl();
        if (webhookUrl.isEmpty()) {
            logger.debug("Webhook URL not configured, skipping alert delivery");
            return;
        }
        
        Timer.Sample sample = Timer.start();
        try {
            Map<String, Object> payload = Map.of(
                "contextId", alert.transaction().contextId(),
                "reason", alert.ruleTriggered(),
                "message", alert.message(),
                "severity", alert.severity(),
                "transaction", Map.of(
                    "transactionId", alert.transaction().transactionId(),
                    "amount", alert.transaction().amount(),
                    "currency", alert.transaction().currency(),
                    "fromAccount", alert.transaction().fromAccount(),
                    "toAccount", alert.transaction().toAccount(),
                    "timestamp", alert.transaction().timestamp()
                )
            );
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            
            restTemplate.postForEntity(webhookUrl, request, String.class);
            webhookSuccessCounter.increment();
            
            logger.info("Alert webhook sent successfully for transaction {} with context {}", 
                alert.transaction().transactionId(), alert.transaction().contextId());
                
        } catch (Exception e) {
            webhookFailureCounter.increment();
            logger.error("Failed to send alert webhook for transaction {} with context {}: {}", 
                alert.transaction().transactionId(), alert.transaction().contextId(), e.getMessage());
        } finally {
            sample.stop(webhookTimer);
        }
    }
}