package com.example.payment.adyen.controller;

import com.adyen.model.notification.NotificationRequest;
import com.adyen.notification.WebhookHandler;
import com.example.payment.adyen.async.AsyncWebhookProcessor;
import com.example.payment.adyen.service.PaymentService;
import com.example.payment.logging.MyLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webhook/adyen")
public class WebhookController {

    private final static MyLogger logger = new MyLogger(LoggerFactory.getLogger(WebhookController.class));

    private final ThreadPoolTaskExecutor taskExecutor;
    private final AsyncWebhookProcessor webhookProcessor;
    private final PaymentService paymentService;
    private final WebhookHandler webhookHandler;

    public WebhookController(ThreadPoolTaskExecutor taskExecutor, AsyncWebhookProcessor webhookProcessor, PaymentService paymentService, WebhookHandler webhookHandler) {
        this.taskExecutor = taskExecutor;
        this.webhookProcessor = webhookProcessor;
        this.paymentService = paymentService;
        this.webhookHandler = webhookHandler;
    }

     @PostMapping
    public ResponseEntity<String> handleAdyenWebhook(HttpServletRequest request) {
         NotificationRequest notificationRequest;
         try {
             String json = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));

             logger.info("New webhook incoming params.", json);

             notificationRequest = webhookHandler.handleNotificationJson(json);

             if (!paymentService.checkAdyenHMAC(notificationRequest) || !paymentService.checkBasicAuthValid(request)) {
                 logger.error("HMAC hash or basic auth is not correct on incoming webhook.");
                 return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
             }
         } catch (Exception e) {
             logger.error("Error on webhook: " + e.getMessage());
             return ResponseEntity.badRequest().body("Invalid payload: " + e.getMessage());
         }

        taskExecutor.execute(() -> webhookProcessor.process(notificationRequest));

        return ResponseEntity.accepted().body("OK");
     }
}
