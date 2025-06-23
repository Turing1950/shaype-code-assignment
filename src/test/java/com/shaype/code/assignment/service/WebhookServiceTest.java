package com.shaype.code.assignment.service;

import com.shaype.code.assignment.config.AlertConfig;
import com.shaype.code.assignment.model.Alert;
import com.shaype.code.assignment.model.Transaction;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    
    private WebhookService webhookService;
    private AlertConfig alertConfig;

    @BeforeEach
    void setUp() {
        alertConfig = new AlertConfig();
        AlertConfig.WebhookConfig webhookConfig = new AlertConfig.WebhookConfig();
        webhookConfig.setUrl("http://test-webhook.com/alerts");
        webhookConfig.setTimeoutMs(5000);
        alertConfig.setWebhook(webhookConfig);
        
        when(restTemplateBuilder.connectTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.readTimeout(any(Duration.class))).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        
        webhookService = new WebhookService(restTemplateBuilder, alertConfig, new SimpleMeterRegistry());
    }

    @Test
    void shouldSendWebhookAlert() {
        Transaction transaction = new Transaction(
            "tx-1", 15000L, "USD", "account-1", "account-2", Instant.now(), "ctx-1"
        );
        Alert alert = new Alert(
            "alert-1", "tx-1", "HIGH_AMOUNT", "HIGH", "Test alert", Instant.now(), transaction
        );

        webhookService.sendAlert(alert);

        verify(restTemplate).postForEntity(eq("http://test-webhook.com/alerts"), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void shouldSkipWhenWebhookUrlEmpty() {
        AlertConfig.WebhookConfig webhookConfig = new AlertConfig.WebhookConfig();
        webhookConfig.setUrl("");
        alertConfig.setWebhook(webhookConfig);
        
        Transaction transaction = new Transaction(
            "tx-1", 15000L, "USD", "account-1", "account-2", Instant.now(), "ctx-1"
        );
        Alert alert = new Alert(
            "alert-1", "tx-1", "HIGH_AMOUNT", "HIGH", "Test alert", Instant.now(), transaction
        );

        webhookService.sendAlert(alert);

        verify(restTemplate, never()).postForEntity(anyString(), any(), any());
    }
}