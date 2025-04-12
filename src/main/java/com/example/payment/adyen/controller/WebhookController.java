package com.example.payment.adyen.controller;

import com.adyen.model.notification.NotificationRequest;
import com.adyen.notification.WebhookHandler;
import com.example.payment.adyen.async.AsyncWebhookProcessor;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.helper.RequestJsonParser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webhook/adyen")
public class WebhookController {

    private ThreadPoolTaskExecutor taskExecutor;
    private AsyncWebhookProcessor webhookProcessor;

    public WebhookController(ThreadPoolTaskExecutor taskExecutor, AsyncWebhookProcessor webhookProcessor) {
        this.taskExecutor = taskExecutor;
        this.webhookProcessor = webhookProcessor;
    }

     @PostMapping
    public ResponseEntity<String> handleAdyenWebhook(HttpServletRequest request) {
         NotificationRequest notificationRequest;
         try {
             String json = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
             WebhookHandler handler = new WebhookHandler();
             notificationRequest = handler.handleNotificationJson(json);
         } catch (Exception e) {
             return ResponseEntity.badRequest().body("Invalid payload: " + e.getMessage());
         }

        taskExecutor.execute(() -> webhookProcessor.process(notificationRequest));

        return ResponseEntity.accepted().body("OK");
     }
}
