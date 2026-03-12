package com.urbanvogue.order_service.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {

    private Long userId;

    private List<OrderItemRequest> items;
}