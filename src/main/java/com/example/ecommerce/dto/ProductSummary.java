package com.example.ecommerce.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductSummary {
    private String id;
    private String name;
    private Double price;
}
