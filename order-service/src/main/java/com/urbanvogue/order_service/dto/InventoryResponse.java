package com.urbanvogue.order_service.dto;

import lombok.Data;

@Data
public class InventoryResponse {

    private Long id;

    private Long productId;

    private Integer availableQuantity;

    private Integer reservedQuantity;

}