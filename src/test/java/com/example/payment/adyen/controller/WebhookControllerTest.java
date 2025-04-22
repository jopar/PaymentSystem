package com.example.payment.adyen.controller;

import com.adyen.model.notification.NotificationRequest;
import com.adyen.notification.WebhookHandler;
import com.example.payment.adyen.async.AsyncWebhookProcessor;
import com.example.payment.adyen.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookControllerTest {
    private ThreadPoolTaskExecutor taskExecutor;
    private AsyncWebhookProcessor webhookProcessor;
    private PaymentService paymentService;
    private WebhookHandler webhookHandler;
    private WebhookController webhookController;

    @BeforeEach
    void setUp() {
        taskExecutor = mock(ThreadPoolTaskExecutor.class);
        webhookProcessor = mock(AsyncWebhookProcessor.class);
        paymentService = mock(PaymentService.class);
        webhookHandler = mock(WebhookHandler.class);

        webhookController = new WebhookController(taskExecutor, webhookProcessor, paymentService, webhookHandler);
    }

    @Test
    void testHandleAdyenWebhookSuccess() throws IOException {
        // Arrange
        String payload = "{ \"live\": \"true\", \"notificationItems\": [] }";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(payload.getBytes());

        NotificationRequest mockNotification = new NotificationRequest();
        mockNotification.setLive("true");
        mockNotification.setNotificationItems(Collections.emptyList());

        when(webhookHandler.handleNotificationJson(payload)).thenReturn(mockNotification);

        when(paymentService.checkAdyenHMAC(mockNotification)).thenReturn(true);
        when(paymentService.checkBasicAuthValid(request)).thenReturn(true);

        // Act
        ResponseEntity<String> response = webhookController.handleAdyenWebhook(request);

        // Assert
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals("OK", response.getBody());

        verify(taskExecutor, times(1)).execute(any(Runnable.class));
    }

    @Test
    void testHandleAdyenWebhookUnauthorized() throws Exception {
        // Arrange
        String payload = "{ \"live\": \"true\", \"notificationItems\": [] }";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(payload.getBytes());

        NotificationRequest mockNotification = new NotificationRequest();
        mockNotification.setLive("true");
        mockNotification.setNotificationItems(Collections.emptyList());

        when(webhookHandler.handleNotificationJson(payload)).thenReturn(mockNotification);

        when(paymentService.checkAdyenHMAC(mockNotification)).thenReturn(false);

        // Act
        ResponseEntity<String> response = webhookController.handleAdyenWebhook(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Unauthorized", response.getBody());

        verify(taskExecutor, never()).execute(any());
    }

    @Test
    void testHandleAdyenWebhookBadRequest() throws IOException {
        // Arrange
        String invalidPayload = "{ invalid json ";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(invalidPayload.getBytes());

        when(webhookHandler.handleNotificationJson(anyString()))
                .thenThrow(new RuntimeException("Failed to parse payload"));

        // Act
        ResponseEntity<String> response = webhookController.handleAdyenWebhook(request);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Invalid payload"));
    }

    @Test
    void testHandleAdyenWebhookUnauthorizedHMAC() {
        // Arrange
        String validPayload = "{ \"merchantAccount\": \"YourMerchantAccount\", \"pspReference\": \"12345\" }";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(validPayload.getBytes());

        when(paymentService.checkAdyenHMAC(any(NotificationRequest.class))).thenReturn(false);
        when(paymentService.checkBasicAuthValid(request)).thenReturn(true);

        // Act
        ResponseEntity<String> response = webhookController.handleAdyenWebhook(request);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("Unauthorized"));
    }
}
