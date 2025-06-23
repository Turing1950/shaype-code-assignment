package com.shaype.code.assignment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "alert")
public class AlertConfig {
    
    private Long amountThreshold = 10000L;
    private Set<String> watchlistAccounts = Set.of();
    private WebhookConfig webhook = new WebhookConfig();
    
    public Long getAmountThreshold() {
        return amountThreshold;
    }
    
    public void setAmountThreshold(Long amountThreshold) {
        this.amountThreshold = amountThreshold;
    }
    
    public Set<String> getWatchlistAccounts() {
        return watchlistAccounts;
    }
    
    public void setWatchlistAccounts(Set<String> watchlistAccounts) {
        this.watchlistAccounts = watchlistAccounts;
    }
    
    public WebhookConfig getWebhook() {
        return webhook;
    }
    
    public void setWebhook(WebhookConfig webhook) {
        this.webhook = webhook;
    }
    
    public static class WebhookConfig {
        private String url = "";
        private int timeoutMs = 5000;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public int getTimeoutMs() {
            return timeoutMs;
        }
        
        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}