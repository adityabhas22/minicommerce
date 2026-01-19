package com.example.ecommerce.webhook;

import com.example.ecommerce.dto.PaymentWebhookRequest;
import com.example.ecommerce.model.PaymentStatus;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/payment")
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final PaymentService paymentService;
    private final OrderService orderService;

    @PostMapping
    public void handleWebhook(@RequestBody PaymentWebhookRequest request) {
        PaymentStatus status = PaymentStatus.valueOf(request.getStatus());
        paymentService.updatePaymentStatus(request.getPaymentId(), status);
        orderService.handlePaymentUpdate(request.getOrderId(), status);
    }
}
