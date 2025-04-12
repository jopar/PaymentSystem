package com.example.payment.adyen.controller;

import com.adyen.model.ApiError;
import com.adyen.model.checkout.*;
import com.adyen.service.exception.ApiException;
import com.example.payment.adyen.service.PaymentService;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.adyen.validator.PaymentValidator;
import com.example.payment.helper.PaymentMethodHelper;
import com.example.payment.helper.RequestJsonParser;
import com.example.payment.model.Payment;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/adyen")
public class PaymentController {

    private final PaymentService paymentService;
    private PaymentValidator paymentValidator;

    public PaymentController(PaymentService paymentService, PaymentValidator paymentValidator) {
        this.paymentService = paymentService;
        this.paymentValidator = paymentValidator;
    }

    @PostMapping("/pay")
    public ResponseEntity<?> processingPayment(HttpServletRequest request) {
        PaymentRequestDTO paymentRequestDTO;
        try {
            paymentRequestDTO = RequestJsonParser.parse(request, PaymentRequestDTO.class);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Invalid request payload: " + e.getMessage());
        }

        // Validate all request data
        List<String> errorsOnValidate = paymentValidator.validateOnPay(paymentRequestDTO);
        if (!errorsOnValidate.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", errorsOnValidate));
        }

        Object paymentMethodObject = paymentRequestDTO.getPaymentMethodDetails();
        String amount = paymentRequestDTO.getAmount();
        String currency = paymentRequestDTO.getCurrency();
        String referenceNumber = paymentRequestDTO.getReferenceNumber();
        String returnURL = paymentRequestDTO.getReturnURL();
        String paymentType = PaymentMethodHelper.getTypeFromPaymentMethod(paymentMethodObject);

        Payment payment = paymentService.createPayment(paymentRequestDTO, paymentType);

        try {
            PaymentResponse paymentResponse;
            try {
                paymentResponse = paymentService.makePayment(payment, paymentMethodObject, amount, currency, referenceNumber, returnURL);
            } catch (ApiException apiException) {
                ApiError apiError = apiException.getError();
                String errorMessage = apiError.getMessage();
                String errorType = apiError.getErrorType();

                paymentService.updatePaymentFailure(payment, errorMessage, errorType);
                return ResponseEntity.badRequest().body(errorMessage);
            } catch (Exception e) {
                paymentService.updatePaymentFailure(payment, "Error: " + e.getMessage());
                return ResponseEntity.badRequest().body("Error: " + e.getMessage());
            }

            // Preveri rezultat in vrni odgovor
            PaymentResponse.ResultCodeEnum resultCode = paymentResponse.getResultCode();
            String authCode = resultCode.getValue();
            if (resultCode.equals(PaymentResponse.ResultCodeEnum.AUTHORISED)) {
                String pspReference = paymentResponse.getPspReference();

                paymentService.updatePaymentSuccess(payment, pspReference, authCode);


                return ResponseEntity.ok("Payment successfully processed");
            } else if (resultCode.equals(PaymentResponse.ResultCodeEnum.REDIRECTSHOPPER)) {
                PaymentResponseAction action = paymentResponse.getAction();
                CheckoutRedirectAction checkoutRedirectAction = action.getCheckoutRedirectAction();
                String redirectActionUrl = checkoutRedirectAction.getUrl();
                paymentService.updatePaymentPending(payment, authCode);

                return ResponseEntity.ok("Payment in process. Redirect to: " + redirectActionUrl);
            } else {
                paymentService.updatePaymentFailure(payment, "Payment failed", authCode);
                return ResponseEntity.badRequest().body("Payment failed: " + paymentResponse.getResultCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/return")
    public ResponseEntity<?> processingCheckout(HttpServletRequest request) {

        // Validate all request data
        List<String> errorsOnValidate = paymentValidator.validateOnReturn(request);
        if (!errorsOnValidate.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", errorsOnValidate));
        }

        Long paymentId = Long.parseLong(request.getParameter("payment_id"));
        String referenceNumber = request.getParameter("reference_number");
        String redirectResult = request.getParameter("redirectResult");

        Payment payment = paymentService.getPaymentByID(paymentId);
        PaymentDetailsResponse paymentDetailsResponse;

        try {
            paymentDetailsResponse = paymentService.checkPayment(referenceNumber, redirectResult);
            String pspReference = paymentDetailsResponse.getPspReference();
            PaymentDetailsResponse.ResultCodeEnum resultCode = paymentDetailsResponse.getResultCode();
            String authCode = resultCode.getValue();

            if (resultCode.equals(PaymentDetailsResponse.ResultCodeEnum.AUTHORISED)) {
                paymentService.updatePaymentSuccess(payment, pspReference, authCode);

                return ResponseEntity.ok("Payment successfully processed");
            } else if (resultCode.equals(PaymentDetailsResponse.ResultCodeEnum.CANCELLED)) {
                paymentService.updatePaymentSuccess(payment, pspReference, authCode);

                return ResponseEntity.ok("Payment was canceled. Continue with order or change different payment method");
            } else  if (resultCode.equals(PaymentDetailsResponse.ResultCodeEnum.ERROR)) {
                paymentService.updatePaymentFailure(payment, paymentDetailsResponse.getRefusalReason(), authCode);

                return ResponseEntity.badRequest().body("Payment failed: " + paymentDetailsResponse.getRefusalReason());
            } else if (resultCode.equals(PaymentDetailsResponse.ResultCodeEnum.PENDING) || resultCode.equals(PaymentDetailsResponse.ResultCodeEnum.RECEIVED)) {
                paymentService.updatePaymentPending(payment, authCode);

                return ResponseEntity.ok("Payment in process. Waiting for payment completed.");
            } else if (resultCode.equals(PaymentDetailsResponse.ResultCodeEnum.REFUSED)) {
                paymentService.updatePaymentFailure(payment, "The payment was refused by the shopper's bank.", authCode);

                return ResponseEntity.badRequest().body("The payment was refused by the shopper's bank. Pleae use different payment method.");
            } else {
                throw new IllegalArgumentException("Unknown result code: " + authCode);
            }
        } catch (ApiException apiException) {
            ApiError apiError = apiException.getError();
            String errorMessage = apiError.getMessage();
            String errorType = apiError.getErrorType();

            paymentService.updatePaymentFailure(payment, errorMessage, errorType);
            return ResponseEntity.badRequest().body(errorMessage);
        } catch (Exception e) {
            paymentService.updatePaymentFailure(payment, "Error: " + e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> processingWebhook(HttpServletRequest request) {
        return ResponseEntity.ok("Works!");
    }
}
