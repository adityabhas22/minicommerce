package com.example.ecommerce.dto;

import com.example.ecommerce.model.OrderStatus;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private String id;
    private String userId;
    private Double totalAmount;
    private OrderStatus status;
    private PaymentSummary payment;
    private List<OrderItemResponse> items;
}
