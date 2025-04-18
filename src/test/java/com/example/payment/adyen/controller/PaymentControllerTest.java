package com.example.payment.adyen.controller;

import com.adyen.model.ApiError;
import com.adyen.model.checkout.CheckoutRedirectAction;
import com.adyen.model.checkout.PaymentDetailsResponse;
import com.adyen.model.checkout.PaymentResponse;
import com.adyen.model.checkout.PaymentResponseAction;
import com.adyen.service.exception.ApiException;
import com.example.payment.adyen.dto.PaymentDTO;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.adyen.service.PaymentService;
import com.example.payment.adyen.validator.PaymentValidator;
import com.example.payment.helper.PaymentMethodHelper;
import com.example.payment.helper.RequestJsonParser;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class PaymentControllerTest {
    private PaymentService paymentService;
    private PaymentValidator paymentValidator;
    private PaymentController paymentController;

    @BeforeEach
    public void setUp() {
        paymentService = mock(PaymentService.class);
        paymentValidator = mock(PaymentValidator.class);

        paymentController = new PaymentController(paymentService, paymentValidator);
    }

    @Test
    void testProcessingPaymentInvalidJsonPayload() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        try (MockedStatic<RequestJsonParser> mockedParser = mockStatic(RequestJsonParser.class)) {
            mockedParser.when(() -> RequestJsonParser.parse(eq(request), eq(PaymentRequestDTO.class)))
                    .thenThrow(new IOException("Malformed JSON"));

            ResponseEntity<?> response = paymentController.processingPayment(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().toString().contains("Invalid request payload"));
        }
    }

    @Test
    void testProcessingPaymentValidationFails() {
        // Create MockHttpServletRequest
        MockHttpServletRequest request = new MockHttpServletRequest();

        String mockRequestBody = "{ \"paymentMethodDetails\": {\"type\": \"scheme\"}, \"amount\": \"1000\", \"currency\": \"USD\", \"referenceNumber\": \"12345\", \"returnURL\": \"https://example.com\" }";
        request.setContent(mockRequestBody.getBytes());

        PaymentRequestDTO paymentRequestDTO = mock(PaymentRequestDTO.class);

        try (MockedStatic<RequestJsonParser> mockedParser = mockStatic(RequestJsonParser.class)) {
            mockedParser.when(() -> RequestJsonParser.parse(request, PaymentRequestDTO.class)).thenReturn(paymentRequestDTO);

            List<String> validationErrors = List.of("Invalid amount", "Invalid payment method");
            when(paymentValidator.validateOnPay(paymentRequestDTO)).thenReturn(validationErrors);

            ResponseEntity<?> response = paymentController.processingPayment(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

            assertTrue(response.getBody().toString().contains("errors"));

            assertTrue(response.getBody().toString().contains("Invalid amount"));
            assertTrue(response.getBody().toString().contains("Invalid payment method"));
        }
    }

    @Test
    void testProcessingPaymentSuccessAuthorised() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        PaymentRequestDTO paymentRequestDTO = mock(PaymentRequestDTO.class);
        PaymentDTO paymentDTO = mock(PaymentDTO.class);
        Object paymentMethod = new Object();
        String amount = "1000";
        String currency = "USD";
        String referenceNumber = "12345";
        String returnUrl = "https://example.com";

        when(paymentRequestDTO.getPaymentMethodDetails()).thenReturn(paymentMethod);
        when(paymentRequestDTO.getAmount()).thenReturn(amount);
        when(paymentRequestDTO.getCurrency()).thenReturn(currency);
        when(paymentRequestDTO.getReferenceNumber()).thenReturn(referenceNumber);
        when(paymentRequestDTO.getReturnURL()).thenReturn(returnUrl);
        when(paymentValidator.validateOnPay(paymentRequestDTO)).thenReturn(List.of());

        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        when(paymentResponse.getResultCode()).thenReturn(PaymentResponse.ResultCodeEnum.AUTHORISED);
        when(paymentResponse.getPspReference()).thenReturn("psp-123");

        try (MockedStatic<RequestJsonParser> mockedParser = mockStatic(RequestJsonParser.class);
             MockedStatic<PaymentMethodHelper> helper = mockStatic(PaymentMethodHelper.class)) {

            mockedParser.when(() -> RequestJsonParser.parse(request, PaymentRequestDTO.class)).thenReturn(paymentRequestDTO);
            helper.when(() -> PaymentMethodHelper.getTypeFromPaymentMethod(paymentMethod)).thenReturn("card");

            when(paymentService.createPayment(paymentRequestDTO, "card")).thenReturn(paymentDTO);
            when(paymentService.makePayment(paymentDTO, paymentMethod, amount, currency, referenceNumber, returnUrl)).thenReturn(paymentResponse);

            ResponseEntity<?> response = paymentController.processingPayment(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().toString().contains("successfully"));
            verify(paymentService).updatePaymentSuccess(paymentDTO, "psp-123", PaymentResponse.ResultCodeEnum.AUTHORISED.getValue());
        }
    }

    @Test
    void testProcessingPaymentRedirectShopper() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        PaymentRequestDTO paymentRequestDTO = mock(PaymentRequestDTO.class);
        PaymentDTO paymentDTO = mock(PaymentDTO.class);
        Object paymentMethod = new Object();
        String amount = "1000";
        String currency = "USD";
        String referenceNumber = "12345";
        String returnUrl = "https://example.com";

        when(paymentRequestDTO.getPaymentMethodDetails()).thenReturn(paymentMethod);
        when(paymentRequestDTO.getAmount()).thenReturn(amount);
        when(paymentRequestDTO.getCurrency()).thenReturn(currency);
        when(paymentRequestDTO.getReferenceNumber()).thenReturn(referenceNumber);
        when(paymentRequestDTO.getReturnURL()).thenReturn(returnUrl);
        when(paymentValidator.validateOnPay(paymentRequestDTO)).thenReturn(List.of());

        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        when(paymentResponse.getResultCode()).thenReturn(PaymentResponse.ResultCodeEnum.REDIRECTSHOPPER);

        PaymentResponseAction action = mock(PaymentResponseAction.class);
        CheckoutRedirectAction redirectAction = mock(CheckoutRedirectAction.class);

        when(paymentResponse.getAction()).thenReturn(action);
        when(action.getCheckoutRedirectAction()).thenReturn(redirectAction);
        when(redirectAction.getUrl()).thenReturn("https://redirect.example.com");

        try (MockedStatic<RequestJsonParser> mockedParser = mockStatic(RequestJsonParser.class);
             MockedStatic<com.example.payment.helper.PaymentMethodHelper> helper = mockStatic(com.example.payment.helper.PaymentMethodHelper.class)) {

            mockedParser.when(() -> RequestJsonParser.parse(request, PaymentRequestDTO.class)).thenReturn(paymentRequestDTO);
            helper.when(() -> com.example.payment.helper.PaymentMethodHelper.getTypeFromPaymentMethod(paymentMethod)).thenReturn("card");

            when(paymentService.createPayment(paymentRequestDTO, "card")).thenReturn(paymentDTO);
            when(paymentService.makePayment(paymentDTO, paymentMethod, amount, currency, referenceNumber, returnUrl)).thenReturn(paymentResponse);

            ResponseEntity<?> response = paymentController.processingPayment(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(response.getBody().toString().contains("Redirect to: https://redirect.example.com"));
            verify(paymentService).updatePaymentPending(paymentDTO, PaymentResponse.ResultCodeEnum.REDIRECTSHOPPER.getValue());
        }
    }

    @Test
    void testProcessingPaymentApiException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();

        PaymentRequestDTO paymentRequestDTO = mock(PaymentRequestDTO.class);
        PaymentDTO paymentDTO = mock(PaymentDTO.class);
        Object paymentMethod = new Object();
        String amount = "1000";
        String currency = "USD";
        String referenceNumber = "12345";
        String returnUrl = "https://example.com";

        when(paymentRequestDTO.getPaymentMethodDetails()).thenReturn(paymentMethod);
        when(paymentRequestDTO.getAmount()).thenReturn(amount);
        when(paymentRequestDTO.getCurrency()).thenReturn(currency);
        when(paymentRequestDTO.getReferenceNumber()).thenReturn(referenceNumber);
        when(paymentRequestDTO.getReturnURL()).thenReturn(returnUrl);
        when(paymentValidator.validateOnPay(paymentRequestDTO)).thenReturn(List.of());

        ApiError apiError = new ApiError();
        apiError.setMessage("API error occurred");
        apiError.setErrorType("validation");

        ApiException apiException = new ApiException("Bad request", 400, new java.util.HashMap<>());
        apiException.setError(apiError);

        try (MockedStatic<RequestJsonParser> mockedParser = mockStatic(RequestJsonParser.class);
             MockedStatic<com.example.payment.helper.PaymentMethodHelper> helper = mockStatic(com.example.payment.helper.PaymentMethodHelper.class)) {

            mockedParser.when(() -> RequestJsonParser.parse(request, PaymentRequestDTO.class)).thenReturn(paymentRequestDTO);
            helper.when(() -> com.example.payment.helper.PaymentMethodHelper.getTypeFromPaymentMethod(paymentMethod)).thenReturn("card");

            when(paymentService.createPayment(paymentRequestDTO, "card")).thenReturn(paymentDTO);
            when(paymentService.makePayment(paymentDTO, paymentMethod, amount, currency, referenceNumber, returnUrl)).thenThrow(apiException);

            ResponseEntity<?> response = paymentController.processingPayment(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertTrue(response.getBody().toString().contains("API error occurred"));
            verify(paymentService).updatePaymentFailure(paymentDTO, "API error occurred", "validation");
        }
    }

    @Test
    void testProcessingCheckoutValidationFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setParameter("payment_id", "1");

        List<String> errors = List.of("Missing redirectResult", "Missing referenceNumber");

        when(paymentValidator.validateOnReturn(request)).thenReturn(errors);

        ResponseEntity<?> response = paymentController.processingCheckout(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("errors"));
        assertTrue(response.getBody().toString().contains("Missing redirectResult"));
        assertTrue(response.getBody().toString().contains("Missing referenceNumber"));
    }

    @Test
    void testProcessingCheckoutSuccessfulAuthorization() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("payment_id", "123");
        request.setParameter("reference_number", "ref123");
        request.setParameter("redirectResult", "redirectValue");

        when(paymentValidator.validateOnReturn(request)).thenReturn(List.of());

        PaymentDTO payment = mock(PaymentDTO.class);
        PaymentDetailsResponse responseMock = mock(PaymentDetailsResponse.class);
        PaymentDetailsResponse.ResultCodeEnum resultCode = PaymentDetailsResponse.ResultCodeEnum.AUTHORISED;

        when(paymentService.getPaymentByID(123L)).thenReturn(payment);
        when(paymentService.checkPayment("ref123", "redirectValue")).thenReturn(responseMock);
        when(responseMock.getPspReference()).thenReturn("psp123");
        when(responseMock.getResultCode()).thenReturn(resultCode);

        ResponseEntity<?> response = paymentController.processingCheckout(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Payment successfully processed"));

        verify(paymentService).updatePaymentSuccess(payment, "psp123", resultCode.getValue());
    }
}
