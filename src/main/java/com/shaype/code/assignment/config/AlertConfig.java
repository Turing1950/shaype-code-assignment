package com.shaype.code.assignment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "alert")
public class AlertConfig {
    
    private Long amountThreshold = 10000L;
    private Set<String> watchlistAccounts = Set.of();
    
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
}