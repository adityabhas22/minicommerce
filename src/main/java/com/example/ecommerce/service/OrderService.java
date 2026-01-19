package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderItemResponse;
import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.dto.PaymentSummary;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.OrderStatus;
import com.example.ecommerce.model.Payment;
import com.example.ecommerce.model.PaymentStatus;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.ProductRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PaymentRepository paymentRepository;

    public OrderResponse createOrder(String userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        List<OrderItem> orderItems = cartItems.stream().map(item -> {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            if (product.getStock() < item.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock");
            }
            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
            return OrderItem.builder()
                    .id(UUID.randomUUID().toString())
                    .orderId(null)
                    .productId(product.getId())
                    .quantity(item.getQuantity())
                    .price(product.getPrice())
                    .build();
        }).collect(Collectors.toList());

        double totalAmount = orderItems.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        Order order = Order.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .totalAmount(totalAmount)
                .status(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .items(orderItems)
                .build();
        orderItems.forEach(item -> item.setOrderId(order.getId()));

        Order saved = orderRepository.save(order);
        cartItemRepository.deleteByUserId(userId);

        return toResponse(saved, null);
    }

    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return toResponse(order, payment);
    }

    public List<OrderResponse> getOrdersByUser(String userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(order -> {
                    Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                    return toResponse(order, payment);
                })
                .collect(Collectors.toList());
    }

    public OrderResponse cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalArgumentException("Paid orders cannot be cancelled");
        }
        if (order.getStatus() != OrderStatus.CANCELLED) {
            restoreStock(order.getItems());
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        return toResponse(order, payment);
    }

    public void updateOrderStatus(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
        order.setStatus(status);
        orderRepository.save(order);
    }

    private OrderResponse toResponse(Order order, Payment payment) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        PaymentSummary paymentSummary = null;
        if (payment != null) {
            paymentSummary = PaymentSummary.builder()
                    .id(payment.getId())
                    .status(payment.getStatus())
                    .amount(payment.getAmount())
                    .build();
        }

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .payment(paymentSummary)
                .items(items)
                .build();
    }

    private void restoreStock(List<OrderItem> items) {
        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found"));
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }

    public void handlePaymentUpdate(String orderId, PaymentStatus status) {
        updateOrderStatus(orderId, status == PaymentStatus.SUCCESS ? OrderStatus.PAID : OrderStatus.FAILED);
    }
}
