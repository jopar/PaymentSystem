package com.example.payment.adyen.controller;

import com.adyen.model.ApiError;
import com.adyen.model.checkout.*;
import com.adyen.service.exception.ApiException;
import com.example.payment.adyen.dto.PaymentDTO;
import com.example.payment.adyen.service.PaymentService;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.adyen.validator.PaymentValidator;
import com.example.payment.helper.PaymentMethodHelper;
import com.example.payment.helper.RequestJsonParser;
import com.example.payment.logging.MyLogger;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/adyen")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentValidator paymentValidator;

    private static final MyLogger logger = new MyLogger(LoggerFactory.getLogger(PaymentController.class));

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
            logger.error("Invalid request payload: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request payload: " + e.getMessage());
        }

        logger.info("New payment request with data:", paymentRequestDTO);

        // Validate all request data
        List<String> errorsOnValidate = paymentValidator.validateOnPay(paymentRequestDTO);
        if (!errorsOnValidate.isEmpty()) {
            logger.error("Validate error on request new payment.", errorsOnValidate);
            return ResponseEntity.badRequest().body(Map.of("errors", errorsOnValidate));
        }

        Object paymentMethodObject = paymentRequestDTO.getPaymentMethodDetails();
        String amount = paymentRequestDTO.getAmount();
        String currency = paymentRequestDTO.getCurrency();
        String referenceNumber = paymentRequestDTO.getReferenceNumber();
        String returnURL = paymentRequestDTO.getReturnURL();
        String paymentType = PaymentMethodHelper.getTypeFromPaymentMethod(paymentMethodObject);

        try {
            PaymentResponse paymentResponse;

            PaymentDTO payment = paymentService.createPayment(paymentRequestDTO, paymentType);
            try {
                paymentResponse = paymentService.makePayment(payment, paymentMethodObject, amount, currency, referenceNumber, returnURL);
            } catch (ApiException apiException) {
                ApiError apiError = apiException.getError();
                String errorMessage = apiError.getMessage();
                String errorType = apiError.getErrorType();

                logger.error("Error on api call to make payment adyen.", apiError);
                paymentService.updatePaymentFailure(payment, errorMessage, errorType);
                return ResponseEntity.badRequest().body(errorMessage);
            } catch (Exception e) {
                logger.error("Error on make payment: " + e.getMessage());
                paymentService.updatePaymentFailure(payment, getErrorMessage(e.getMessage()));
                return ResponseEntity.badRequest().body(getErrorMessage(e.getMessage()));
            }

            PaymentResponse.ResultCodeEnum resultCode = paymentResponse.getResultCode();
            String authCode = resultCode.getValue();


            return switch (resultCode) {
                case AUTHORISED -> getStringResponseEntityAuthorised(paymentResponse, payment, authCode);
                case REDIRECTSHOPPER -> getStringResponseEntityRedirectShopper(paymentResponse, payment, authCode);
                case RECEIVED, PENDING, PRESENTTOSHOPPER ->
                        getStringResponseEntityIntermediateResult(paymentResponse, payment, authCode);
                case CANCELLED, ERROR, REFUSED -> getStringResponseEntityError(paymentResponse, payment, authCode);
                default -> throw new IllegalArgumentException("Unknown result code: " + authCode);
            };
        } catch (Exception e) {
            logger.error("Error on payment controller: " + e);
            return ResponseEntity.badRequest().body(getErrorMessage(e.getMessage()));
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

        HashMap<String, String> requestedLogData = new HashMap<>();
        requestedLogData.put("reference_number", referenceNumber);
        requestedLogData.put("redirectResult", redirectResult);

        logger.info("New return request with data:", requestedLogData);

        PaymentDTO payment = paymentService.getPaymentByID(paymentId);
        PaymentDetailsResponse paymentDetailsResponse;

        try {
            paymentDetailsResponse = paymentService.checkPayment(redirectResult);
            String pspReference = paymentDetailsResponse.getPspReference();
            PaymentDetailsResponse.ResultCodeEnum resultCode = paymentDetailsResponse.getResultCode();
            String authCode = resultCode.getValue();

            ResponseEntity<String> response;

            switch (resultCode) {
                case AUTHORISED:
                    paymentService.updatePaymentSuccess(payment, pspReference, authCode);

                    logger.info("Payment successfully processed!");
                    response = ResponseEntity.ok("Payment successfully processed");
                    break;
                case CANCELLED:
                    paymentService.updatePaymentSuccess(payment, pspReference, authCode);

                    logger.info("Payment was canceled. Continue with order or change different payment method.");
                    response = ResponseEntity.ok("Payment was canceled. Continue with order or change different payment method");
                    break;
                case ERROR:
                    paymentService.updatePaymentFailure(payment, paymentDetailsResponse.getRefusalReason(), pspReference, authCode);

                    logger.error("Payment failed: " + paymentDetailsResponse.getRefusalReason());
                    response = ResponseEntity.badRequest().body("Payment failed: " + paymentDetailsResponse.getRefusalReason());
                    break;
                case PENDING, RECEIVED:
                    paymentService.updatePaymentPending(payment, authCode, pspReference);

                    logger.info("Payment in process. Waiting for payment completed.");
                    response = ResponseEntity.ok("Payment in process. Waiting for payment completed.");
                    break;
                case REFUSED:
                    paymentService.updatePaymentFailure(payment, "The payment was refused by the shopper's bank.", pspReference, authCode);

                    logger.info("The payment was refused by the shopper's bank. Please use different payment method.");
                    response = ResponseEntity.badRequest().body("The payment was refused by the shopper's bank. Please use different payment method.");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown result code: " + authCode);

            }

            return response;
        } catch (ApiException apiException) {
            ApiError apiError = apiException.getError();
            String errorMessage = apiError.getMessage();
            String errorType = apiError.getErrorType();

            logger.error("Error on api call to check payment adyen.", apiError);
            paymentService.updatePaymentFailure(payment, errorMessage, errorType);
            return ResponseEntity.badRequest().body(errorMessage);
        } catch (Exception e) {
            logger.error("Error on check payment: " + e.getMessage());
            paymentService.updatePaymentFailure(payment, getErrorMessage(e.getMessage()));
            return ResponseEntity.badRequest().body(getErrorMessage(e.getMessage()));
        }
    }

    private ResponseEntity<String> getStringResponseEntityIntermediateResult(PaymentResponse paymentResponse, PaymentDTO payment, String authCode) {
        String pspReference = paymentResponse.getPspReference();
        paymentService.updatePaymentPending(payment, authCode, pspReference);
        logger.info("The payment received but waiting for final status.");

        return ResponseEntity.ok("The payment received but waiting for final status.");
    }

    private ResponseEntity<String> getStringResponseEntityError(PaymentResponse paymentResponse, PaymentDTO payment, String authCode) {
        logger.error("The payment failed. Auth code: " + authCode);
        paymentService.updatePaymentFailure(payment, "Payment failed", authCode);
        return ResponseEntity.badRequest().body("The payment failed: " + paymentResponse.getResultCode());
    }

    private ResponseEntity<String> getStringResponseEntityRedirectShopper(PaymentResponse paymentResponse, PaymentDTO payment, String authCode) {
        PaymentResponseAction action = paymentResponse.getAction();
        CheckoutRedirectAction checkoutRedirectAction = action.getCheckoutRedirectAction();
        String redirectActionUrl = checkoutRedirectAction.getUrl();
        paymentService.updatePaymentPending(payment, authCode);

        logger.info("The payment in process. Redirect to: " + redirectActionUrl);
        return ResponseEntity.ok("v in process. Redirect to: " + redirectActionUrl);
    }

    private ResponseEntity<String> getStringResponseEntityAuthorised(PaymentResponse paymentResponse, PaymentDTO payment, String authCode) {
        String pspReference = paymentResponse.getPspReference();

        paymentService.updatePaymentSuccess(payment, pspReference, authCode);

        logger.info("The payment successfully processed!");
        return ResponseEntity.ok("The payment successfully processed");
    }

    private String getErrorMessage(String errorMessage) {
        return String.format("Error: %s", errorMessage);
    }
}
