package com.example.ecommerce.client;

import com.example.ecommerce.dto.PaymentWebhookRequest;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class MockPaymentClient {
    private final RestTemplate restTemplate;

    public void triggerWebhook(String webhookUrl, PaymentWebhookRequest request) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(3000);
                restTemplate.postForEntity(webhookUrl, request, Void.class);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
    }
}
