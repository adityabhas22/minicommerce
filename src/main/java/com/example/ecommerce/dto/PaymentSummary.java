package com.example.ecommerce.dto;

import com.example.ecommerce.model.PaymentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentSummary {
    private String id;
    private PaymentStatus status;
    private Double amount;
}
