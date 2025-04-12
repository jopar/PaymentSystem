package com.example.payment.adyen.async;

import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.example.payment.adyen.service.PaymentService;
import com.example.payment.model.Payment;

public class AsyncWebhookProcessor {
    private PaymentService paymentService;

    public AsyncWebhookProcessor(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void process(NotificationRequest request) {
        for (NotificationRequestItem item : request.getNotificationItems()) {
            paymentService.handleNotification(item);
//            System.out.println("Processing notification: " + item.getEventCode());
//            String pspReference = item.getPspReference();
//            String ma = item.getMerchantReference();
//            String eventCode = item.getEventCode();
//
//            Payment payment = paymentService.getPaymentByPspReference(pspReference);

        }

        System.out.println("Process all notification.");
    }
}
