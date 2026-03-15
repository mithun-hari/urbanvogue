package com.urbanvogue.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentResponse {

    private String transactionId;

    private String paymentStatus;
}