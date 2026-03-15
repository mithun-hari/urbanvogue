package com.urbanvogue.order_service.dto;

public class PaymentRequest {

    private Long orderId;
    private Double amount;
    private String paymentMethod;

    public PaymentRequest(Long orderId, Double amount, String paymentMethod) {
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Double getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }
}