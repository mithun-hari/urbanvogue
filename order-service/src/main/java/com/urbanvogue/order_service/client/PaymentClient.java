package com.urbanvogue.order_service.client;

import com.urbanvogue.order_service.dto.PaymentRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PaymentClient {

    private final WebClient webClient;

    public PaymentClient() {
        this.webClient = WebClient.create("http://localhost:8087");
    }

    public String processPayment(Long orderId, Double amount) {

        String response = webClient.post()
                .uri("/payments")
                .bodyValue(new PaymentRequest(orderId, amount, "CARD"))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return response;
    }
}