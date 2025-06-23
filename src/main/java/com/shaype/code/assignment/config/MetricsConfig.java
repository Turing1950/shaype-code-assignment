package com.shaype.code.assignment.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public KafkaMetrics kafkaMetrics(MeterRegistry registry) {
        return new KafkaMetrics(registry);
    }
}