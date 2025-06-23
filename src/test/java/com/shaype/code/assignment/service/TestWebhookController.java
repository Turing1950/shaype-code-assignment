package com.shaype.code.assignment.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

@RestController
public class TestWebhookController {
    
    public static Map<String, Object> receivedWebhook;
    public static CountDownLatch webhookLatch;
    
    @PostMapping("/test-webhook")
    public ResponseEntity<String> receiveWebhook(@RequestBody Map<String, Object> payload) {
        receivedWebhook = payload;
        if (webhookLatch != null) {
            webhookLatch.countDown();
        }
        return ResponseEntity.ok("OK");
    }
}