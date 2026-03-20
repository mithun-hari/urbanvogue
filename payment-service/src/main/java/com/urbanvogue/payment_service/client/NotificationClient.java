package com.urbanvogue.payment_service.client;

import com.urbanvogue.payment_service.dto.EmailRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "http://localhost:8088")
public interface NotificationClient {

    @PostMapping("/api/notifications/send-email")
    void sendEmail(@RequestBody EmailRequest request);
}
