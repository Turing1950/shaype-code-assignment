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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookRetryTest {

    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private RestTemplateBuilder restTemplateBuilder;
    
    private WebhookService webhookService;
    private AlertConfig alertConfig;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        alertConfig = new AlertConfig();
        AlertConfig.WebhookConfig webhookConfig = new AlertConfig.WebhookConfig();
        webhookConfig.setUrl("http://test-webhook");
        webhookConfig.setTimeoutMs(1000);
        alertConfig.setWebhook(webhookConfig);
        
        when(restTemplateBuilder.connectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.readTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        
        meterRegistry = new SimpleMeterRegistry();
        webhookService = new WebhookService(restTemplateBuilder, alertConfig, meterRegistry);
    }

    @Test
    void shouldRetryOn5xxErrors() {
        Transaction transaction = new Transaction(
            "tx-1", 15000L, "USD", "account-1", "account-2", Instant.now(), "ctx-1"
        );
        
        Alert alert = new Alert(
            "alert-1", "tx-1", "HIGH_AMOUNT", "HIGH", "Test alert", Instant.now(), transaction
        );

        when(restTemplate.postForEntity(eq("http://test-webhook"), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        webhookService.sendAlert(alert);

        verify(restTemplate, times(1)).postForEntity(eq("http://test-webhook"), any(HttpEntity.class), eq(String.class));
        assertEquals(1, meterRegistry.get("webhook.alerts.5xx.total").counter().count());
        assertEquals(1, meterRegistry.get("webhook.alerts.failure.total").counter().count());
    }
    
    @Test
    void shouldNotRetryOn4xxErrors() {
        Transaction transaction = new Transaction(
            "tx-2", 15000L, "USD", "account-1", "account-2", Instant.now(), "ctx-2"
        );
        
        Alert alert = new Alert(
            "alert-2", "tx-2", "HIGH_AMOUNT", "HIGH", "Test alert", Instant.now(), transaction
        );

        when(restTemplate.postForEntity(eq("http://test-webhook"), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        webhookService.sendAlert(alert);

        verify(restTemplate, times(1)).postForEntity(eq("http://test-webhook"), any(HttpEntity.class), eq(String.class));
        assertEquals(1, meterRegistry.get("webhook.alerts.4xx.total").counter().count());
        assertEquals(1, meterRegistry.get("webhook.alerts.failure.total").counter().count());
    }
    
    @Test
    void shouldRetryOnConnectionErrors() {
        Transaction transaction = new Transaction(
            "tx-3", 15000L, "USD", "account-1", "account-2", Instant.now(), "ctx-3"
        );
        
        Alert alert = new Alert(
            "alert-3", "tx-3", "HIGH_AMOUNT", "HIGH", "Test alert", Instant.now(), transaction
        );

        when(restTemplate.postForEntity(eq("http://test-webhook"), any(HttpEntity.class), eq(String.class)))
            .thenThrow(new ResourceAccessException("Connection timeout"));

        webhookService.sendAlert(alert);

        verify(restTemplate, times(1)).postForEntity(eq("http://test-webhook"), any(HttpEntity.class), eq(String.class));
        assertEquals(1, meterRegistry.get("webhook.alerts.failure.total").counter().count());
    }
}