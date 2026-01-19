package com.example.ecommerce.service;

import com.example.ecommerce.dto.AddToCartRequest;
import com.example.ecommerce.dto.CartItemResponse;
import com.example.ecommerce.dto.ProductSummary;
import com.example.ecommerce.model.CartItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartItemRepository;
import com.example.ecommerce.repository.ProductRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartItem addToCart(AddToCartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        if (product.getStock() == null || product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock");
        }
        CartItem cartItem = cartItemRepository.findByUserIdAndProductId(
                        request.getUserId(), request.getProductId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + request.getQuantity());
                    return existing;
                })
                .orElseGet(() -> CartItem.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(request.getUserId())
                        .productId(request.getProductId())
                        .quantity(request.getQuantity())
                        .build());
        return cartItemRepository.save(cartItem);
    }

    public List<CartItemResponse> getCart(String userId) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        return items.stream().map(item -> {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            ProductSummary summary = null;
            if (product != null) {
                summary = ProductSummary.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .price(product.getPrice())
                        .build();
            }
            return CartItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .product(summary)
                    .build();
        }).collect(Collectors.toList());
    }

    public void clearCart(String userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
