package com.shaype.code.assignment.service;

import com.shaype.code.assignment.config.KafkaMetrics;
import com.shaype.code.assignment.model.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;

@Service
@Primary
@Profile("test")
public class TestTransactionConsumerService extends TransactionConsumerService {

    private final CountDownLatch latch;

    @Autowired
    public TestTransactionConsumerService(KafkaMetrics metrics, CountDownLatch latch) {
        super(metrics);
        this.latch = latch;
    }

    @Override
    @KafkaListener(topics = "${kafka.topic.transactions}")
    public void consume(Transaction transaction) {
        super.consume(transaction);
        latch.countDown();
    }
}