package com.example.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AddToCartRequest {
    @NotBlank
    private String userId;
    @NotBlank
    private String productId;
    @NotNull
    @Positive
    private Integer quantity;
}
