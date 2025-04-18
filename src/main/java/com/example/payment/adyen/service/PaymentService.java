package com.example.payment.adyen.service;


import com.adyen.model.RequestOptions;
import com.adyen.model.checkout.*;
import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.service.checkout.PaymentsApi;
import com.adyen.service.exception.ApiException;
import com.adyen.util.HMACValidator;
import com.example.payment.adyen.dao.PaymentDao;
import com.example.payment.adyen.dao.PaymentWebhookDao;
import com.example.payment.adyen.dto.PaymentDTO;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.adyen.dto.PaymentWebhookDTO;
import com.example.payment.config.AdyenConfig;
import com.example.payment.exceptions.PaymentNotFoundException;
import com.example.payment.helper.PaymentMethodHelper;
import com.example.payment.logging.MyLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PaymentService {

    private final static MyLogger logger = new MyLogger(LoggerFactory.getLogger(PaymentService.class));

    private final PaymentsApi paymentsApi;
    private final AdyenConfig adyenConfig;
    private final PaymentDao paymentDao;
    private final PaymentWebhookDao paymentWebhookDao;

    private HMACValidator hmacValidator;

    public PaymentService(PaymentsApi paymentsApi, AdyenConfig adyenConfig, PaymentDao paymentDao, PaymentWebhookDao paymentWebhookDao) {
        this.paymentsApi = paymentsApi;
        this.adyenConfig = adyenConfig;
        this.paymentDao = paymentDao;
        this.paymentWebhookDao = paymentWebhookDao;
    }

    public void setHmacValidator(HMACValidator hmacValidator) {
        this.hmacValidator = hmacValidator;
    }

    public PaymentResponse makePayment(PaymentDTO payment, Object paymentDetails, String amount, String currency, String referenceNumber, String returnUrl) throws IOException, ApiException {
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

    public PaymentDTO createPayment(PaymentRequestDTO paymentRequestDTO, String paymentType) throws PaymentNotFoundException {
        PaymentDTO payment = new PaymentDTO();
        Date currentDate = new Date();
        payment.setMerchantReference(adyenConfig.getMerchantAccount());
        payment.setAmount(Long.parseLong(paymentRequestDTO.getAmount()));
        payment.setCurrency(paymentRequestDTO.getCurrency());
        payment.setReference(paymentRequestDTO.getReferenceNumber());
        payment.setPaymentMethod(paymentType);
        payment.setStatus("INITIATED");
        payment.setCreateAt(currentDate);
        payment.setUpdateAt(currentDate);

        return paymentDao.insert(payment).orElseThrow(() -> new PaymentNotFoundException());
    }

    public void updatePaymentSuccess(PaymentDTO payment, String pspReference, String authCode) {
        payment.setPspReference(pspReference);
        payment.setAuthCode(authCode);
        payment.setStatus("SUCCESS");
        paymentDao.updatePspReferenceStatusAndCode(payment);
    }

    public void updatePaymentPending(PaymentDTO payment, String authCode) {
        payment.setAuthCode(authCode);
        payment.setStatus("PENDING");
        paymentDao.updateStatusAndAuth(payment);
    }

    public void updatePaymentPending(PaymentDTO payment, String authCode, String pspReference) {
        payment.setAuthCode(authCode);
        payment.setStatus("PENDING");
        payment.setPspReference(pspReference);
        paymentDao.updatePspReferenceStatusAndCode(payment);
    }

    public void updatePaymentFailure(PaymentDTO payment, String errorMessage) {
        payment.setStatus("FAILED");
        payment.setFailureMessage(errorMessage);

        paymentDao.updateStatusAndSetMessage(payment);
    }

    public void updatePaymentFailure(PaymentDTO payment, String errorMessage, String authCode) {
        if (authCode.isEmpty()) {
            authCode = PaymentResponse.ResultCodeEnum.ERROR.getValue();
        }
        payment.setStatus("FAILED");
        payment.setAuthCode(authCode);
        payment.setFailureMessage(errorMessage);

        paymentDao.updateStatusAuthCodeAndSetMessage(payment);
    }

    public void updatePaymentFailure(PaymentDTO payment, String errorMessage, String pspReference, String authCode) {
        updatePaymentFailure(payment, errorMessage, authCode);
        payment.setPspReference(pspReference);
        paymentDao.updatePspReferenceStatusAndCode(payment);
    }

    public PaymentDTO getPaymentByID(Long paymentId) {
        return paymentDao.findById(paymentId).orElse(null);
    }

    public PaymentDTO getPaymentByReferenceNumber(String referenceNumber) {
        return paymentDao.findByReference(referenceNumber).orElse(null);
    }

    public PaymentDTO getPaymentByPspReference(String pspReference) {
        return paymentDao.findByPspReference(pspReference).orElse(null);
    }

    public void handleNotification(NotificationRequestItem item) {
        try {
            String eventCode = item.getEventCode();
            boolean isSuccess = item.isSuccess();
            String merchantReference = item.getMerchantReference();
            String pspReference = item.getPspReference();
            String originalReference = item.getOriginalReference();
            Date eventDate = item.getEventDate();

            Optional<PaymentDTO> paymentOpt = paymentDao.findByPspReference(pspReference);
            if (paymentOpt.isEmpty()) {
                logger.error(String.format("Payment with pspReference %s not exist", pspReference));
                return;
            }

            PaymentDTO payment = paymentOpt.get();

            PaymentWebhookDTO paymentWebhook = new PaymentWebhookDTO();
            paymentWebhook.setPaymentId(payment.getId());
            paymentWebhook.setEventCode(eventCode);
            paymentWebhook.setPspReference(pspReference);
            paymentWebhook.setSuccess(isSuccess);
            paymentWebhook.setReceivedAt(new Date());
            paymentWebhook.setEventDate(eventDate);
            paymentWebhook.setRawNotification(toJson(item));

            paymentWebhookDao.insert(paymentWebhook);

            switch (eventCode) {
                case "AUTHORISATION":
                    if (isSuccess) {
                        payment.setAuthCode("AUTHORISED");
//                    payment.setPspReference(pspReference);
                    } else {
                        payment.setAuthCode("REFUSED");
                    }
                    break;
                case "REFUND":
                    if (isSuccess) payment.setAuthCode("REFUNDED");
                    break;
                case "CANCELLATION":
                    if (isSuccess) payment.setAuthCode("CANCELLED");
                    break;
                case "EXPIRE":
                    if (isSuccess) payment.setAuthCode("EXPIRED");
                    break;
            }

            payment.setStatus("SUCCESS");

            paymentDao.updateStatusAndAuth(payment);
        } catch (Exception e) {
            logger.error("Error when receive new notification: " + e.getMessage(), item);

            throw new RuntimeException("Set for rollback", e);
        }
    }

    private String toJson(NotificationRequestItem item) {
        try {
            return new ObjectMapper().writeValueAsString(item);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public boolean checkBasicAuthValid(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            String encodedCredentials = authHeader.substring(6);
            String decodedCredentials = new String(Base64.getDecoder().decode(encodedCredentials));
            StringTokenizer tokenizer = new StringTokenizer(decodedCredentials, ":");
            String username = tokenizer.nextToken();
            String password = tokenizer.nextToken();

            boolean isOk = username.equals(adyenConfig.getWebhookUsername()) && password.equals(adyenConfig.getWebhookPassword());
            if (!isOk) {
                HashMap<String, String> context = new HashMap<>();
                context.put("incomeUsername", username);
                context.put("incomePassword", password);
                context.put("ourUsername", adyenConfig.getWebhookUsername());
                context.put("ourPassword", adyenConfig.getWebhookPassword());
                logger.error("This webhook callback is unauthorized.", context);
            }
            return isOk;
        }

        return false;
    }

    public boolean checkAdyenHMAC(NotificationRequest notificationRequest) {
        try {
            for (NotificationRequestItem item : notificationRequest.getNotificationItems()) {
                if (!hmacValidator.validateHMAC(item, adyenConfig.getWebhookHMAC())) {
                    throw new RuntimeException("Wrong HMAC for request.");
                }
            }
        } catch (Exception e) {
            logger.error("Wrong calculated HMAC");
            return false;
        }

        return true;
    }
}
