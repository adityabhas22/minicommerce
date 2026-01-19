package com.example.ecommerce.service;

import com.example.ecommerce.client.MockPaymentClient;
import com.example.ecommerce.dto.PaymentRequest;
import com.example.ecommerce.dto.PaymentWebhookRequest;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderStatus;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.PaymentStatus;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final MockPaymentClient mockPaymentClient;

    @Value("${app.payment.webhook-url:http://localhost:8080/api/webhooks/payment}")
    private String webhookUrl;

    public Payment createPayment(PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new IllegalArgumentException("Order is not eligible for payment");
        }

        Payment payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(order.getId())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .paymentId("pay_" + UUID.randomUUID())
                .createdAt(Instant.now())
                .build();
        Payment saved = paymentRepository.save(payment);

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest(
                order.getId(), saved.getPaymentId(), PaymentStatus.SUCCESS.name());
        mockPaymentClient.triggerWebhook(webhookUrl, webhookRequest);

        return saved;
    }

    public void updatePaymentStatus(String paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        payment.setStatus(status);
        paymentRepository.save(payment);
    }
}
