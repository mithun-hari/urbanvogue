package com.urbanvogue.notification_service.messaging;

import com.urbanvogue.notification_service.dto.EmailRequest;
import com.urbanvogue.notification_service.dto.OrderPaidEvent;
import com.urbanvogue.notification_service.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderPaidListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderPaidListener orderPaidListener;

    @Test
    @DisplayName("handleOrderPaidEvent - Sends email with correct recipient")
    void handleOrderPaidEvent_SendsEmail() {
        OrderPaidEvent event = new OrderPaidEvent(42L, 199.99, "customer@example.com");

        orderPaidListener.handleOrderPaidEvent(event);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).sendEmail(captor.capture());

        EmailRequest sentEmail = captor.getValue();
        assertEquals("customer@example.com", sentEmail.getTo());
    }

    @Test
    @DisplayName("handleOrderPaidEvent - Subject contains order ID")
    void handleOrderPaidEvent_CorrectSubject() {
        OrderPaidEvent event = new OrderPaidEvent(42L, 199.99, "customer@example.com");

        orderPaidListener.handleOrderPaidEvent(event);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).sendEmail(captor.capture());

        assertTrue(captor.getValue().getSubject().contains("42"));
    }

    @Test
    @DisplayName("handleOrderPaidEvent - Body contains payment amount")
    void handleOrderPaidEvent_CorrectBody() {
        OrderPaidEvent event = new OrderPaidEvent(42L, 199.99, "customer@example.com");

        orderPaidListener.handleOrderPaidEvent(event);

        ArgumentCaptor<EmailRequest> captor = ArgumentCaptor.forClass(EmailRequest.class);
        verify(emailService).sendEmail(captor.capture());

        assertTrue(captor.getValue().getBody().contains("199.99"));
    }
}
