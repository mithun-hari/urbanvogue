package com.urbanvogue.product_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;

}