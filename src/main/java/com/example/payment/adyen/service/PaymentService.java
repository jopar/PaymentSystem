package com.example.payment.adyen.service;


import com.adyen.model.RequestOptions;
import com.adyen.model.checkout.*;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.service.checkout.PaymentsApi;
import com.adyen.service.exception.ApiException;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.config.AdyenConfig;
import com.example.payment.helper.PaymentMethodHelper;
import com.example.payment.model.Payment;
import com.example.payment.model.PaymentWebhook;
import com.example.payment.repository.PaymentRepository;
import com.example.payment.repository.PaymentWebhookRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class PaymentService {

    private final PaymentsApi paymentsApi;
    private final AdyenConfig adyenConfig;
//    private final PaymentRequest paymentRequest;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookRepository paymentWebhookRepository;

    public PaymentService(PaymentsApi paymentsApi, AdyenConfig adyenConfig, PaymentRepository paymentRepository, PaymentWebhookRepository paymentWebhookRepository) {
        this.paymentsApi = paymentsApi;
        this.adyenConfig = adyenConfig;
        this.paymentRepository = paymentRepository;
        this.paymentWebhookRepository = paymentWebhookRepository;
    }

    public String getServiceName() {
        return "PaymentService";
    }

    public PaymentResponse makePayment(Payment payment, Object paymentDetails, String amount, String currency, String referenceNumber, String returnUrl) throws IOException, ApiException {
        PaymentRequest paymentRequest = new PaymentRequest();

        Amount paymentAmount = new Amount().currency(currency).value(Long.parseLong(amount));
        CheckoutPaymentMethod checkoutPaymentMethod = PaymentMethodHelper.createCheckoutPaymentMethod(paymentDetails);

        String encodePaymentId = URLEncoder.encode(payment.getId().toString(), StandardCharsets.UTF_8);
        String encodePaymentReferenceNumber = URLEncoder.encode(referenceNumber, StandardCharsets.UTF_8);

        String completeReturnUrl = "%s?payment_id=%s&reference_number=%s".formatted(returnUrl, encodePaymentId, encodePaymentReferenceNumber);

        paymentRequest.setMerchantAccount(adyenConfig.getMerchantAccount());
        paymentRequest.setAmount(paymentAmount);
        paymentRequest.setReference(referenceNumber);
        paymentRequest.setPaymentMethod(checkoutPaymentMethod);
        paymentRequest.setReturnUrl(completeReturnUrl);
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setIdempotencyKey(UUID.randomUUID().toString());
        return paymentsApi.payments(paymentRequest, requestOptions);
    }

    public PaymentDetailsResponse checkPayment(String referenceNumber, String redirectResult) throws IOException, ApiException {
        DetailsRequest detailsRequest = new DetailsRequest();
        detailsRequest.setDetails(new PaymentCompletionDetails().redirectResult(redirectResult));

        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setIdempotencyKey(UUID.randomUUID().toString());
        return paymentsApi.paymentsDetails(detailsRequest, requestOptions);
    }

    public Payment createPayment(PaymentRequestDTO paymentRequestDTO, String paymentType) {
        Payment payment = new Payment();
        payment.setMerchantReference(adyenConfig.getMerchantAccount());
        payment.setAmount(Long.parseLong(paymentRequestDTO.getAmount()));
        payment.setCurrency(paymentRequestDTO.getCurrency());
        payment.setReference(paymentRequestDTO.getReferenceNumber());
        payment.setPaymentMethod(paymentType);
        payment.setStatus("INITIATED");
        return paymentRepository.save(payment);
    }

    public void updatePaymentSuccess(Payment payment, String pspReference, String authCode) {
        payment.setPspReference(pspReference);
        payment.setAuthCode(authCode);
        payment.setStatus("SUCCESS");
        paymentRepository.save(payment);
    }

    public void updatePaymentPending(Payment payment, String authCode) {
        payment.setAuthCode(authCode);
        payment.setStatus("PENDING");
        paymentRepository.save(payment);
    }

    public void updatePaymentFailure(Payment payment, String errorMessage) {
        payment.setStatus("FAILED");
        payment.setFailureMessage(errorMessage);
        paymentRepository.save(payment);
    }

    public void updatePaymentFailure(Payment payment, String errorMessage, String authCode) {
        if (authCode.isEmpty()) {
            authCode = PaymentResponse.ResultCodeEnum.ERROR.getValue();
        }
        payment.setStatus("FAILED");
        payment.setAuthCode(authCode);
        payment.setFailureMessage(errorMessage);
        paymentRepository.save(payment);
    }

    public Payment getPaymentByID(Long paymentId) {
        return paymentRepository.getReferenceById(paymentId);
    }

    public Payment getPaymentByReferenceNumber(String referenceNumber) {
        return paymentRepository.findByReference(referenceNumber);
    }

    public Payment getPaymentByPspReference(String pspReference) {
        Optional<Payment> paymentOptional = paymentRepository.findByPspReference(pspReference);
        if (paymentOptional.isEmpty()) {
            return null;
        }

        return paymentOptional.get();
    }

    public void handleNotification(NotificationRequestItem item) {
        String eventCode = item.getEventCode();
        boolean isSuccess = item.isSuccess();
        String merchantReference = item.getMerchantReference();
        String pspReference = item.getPspReference();
        String originalReference = item.getOriginalReference();
        Date eventDate = item.getEventDate();

        Optional<Payment> paymentOpt = paymentRepository.findByPspReference(pspReference);
        if (paymentOpt.isEmpty()) {
            System.out.println("Payment with pspReference " + pspReference + "not exist!" );
            return;
        }

        Payment payment = paymentOpt.get();

        PaymentWebhook paymentWebhook = new PaymentWebhook();
        paymentWebhook.setPayment(payment);
        paymentWebhook.setEventCode(eventCode);
        paymentWebhook.setPspReference(pspReference);
        paymentWebhook.setSuccess(isSuccess);
        paymentWebhook.setReceivedAt(LocalDateTime.now());
        paymentWebhook.setEventDate(eventDate);
        paymentWebhook.setRawNotification(this.toJson(item));

        paymentWebhookRepository.save(paymentWebhook);

        switch (eventCode) {
            case "AUTHORISATION":
                if (isSuccess) {
                    payment.setStatus("AUTHORISED");
                    payment.setPspReference(pspReference);
                } else {
                    payment.setStatus("REFUSED");
                }
                break;
            case "REFUND":
                if (isSuccess) payment.setStatus("REFUNDED");
                break;
            case "CANCELLATION":
                if (isSuccess) payment.setStatus("CANCELLED");
                break;
            case "EXPIRE":
                if (isSuccess) payment.setStatus("EXPIRED");
                break;
            // dodaj ostale evente po potrebi
        }

        payment.setUpdateAt(new Date());
        paymentRepository.save(payment);
    }

    // TODO: 11. 04. 2025 Premakni kam drugam
    private String toJson(NotificationRequestItem item) {
        try {
            return new ObjectMapper().writeValueAsString(item);
        } catch (JsonProcessingException e) {
            // Logiraj napako ali vrzi RuntimeException
            return "{}";
        }
    }
}
