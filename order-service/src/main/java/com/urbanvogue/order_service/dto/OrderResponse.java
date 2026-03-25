package com.urbanvogue.order_service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {

    private Long orderId;

    private Double totalAmount;

    private String status;

    private String paymentUrl;
}