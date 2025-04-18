package com.example.payment.adyen.async;

import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.example.payment.adyen.service.PaymentService;
import com.example.payment.logging.MyLogger;
import org.slf4j.LoggerFactory;

public class AsyncWebhookProcessor {

    private final MyLogger logger = new MyLogger(LoggerFactory.getLogger(AsyncWebhookProcessor.class));
    private final PaymentService paymentService;

    public AsyncWebhookProcessor(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void process(NotificationRequest request) {
        for (NotificationRequestItem item : request.getNotificationItems()) {
            logger.info("Processing webhook for notification request item.", item);

            paymentService.handleNotification(item);
        }

        logger.info("Done with processing webhook");
    }
}
