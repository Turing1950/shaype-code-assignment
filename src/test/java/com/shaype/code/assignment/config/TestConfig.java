package com.shaype.code.assignment.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.CountDownLatch;

@Configuration
@Profile("test")
public class TestConfig {
    
    @Bean
    public CountDownLatch latch() {
        return new CountDownLatch(1);
    }
    
    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}