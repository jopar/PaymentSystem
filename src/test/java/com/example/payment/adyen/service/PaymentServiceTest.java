package com.example.payment.adyen.service;

import com.adyen.model.RequestOptions;
import com.adyen.model.checkout.CheckoutPaymentMethod;
import com.adyen.model.checkout.PaymentRequest;
import com.adyen.model.checkout.PaymentResponse;
import com.adyen.model.notification.Amount;
import com.adyen.model.notification.NotificationRequest;
import com.adyen.model.notification.NotificationRequestItem;
import com.adyen.service.checkout.PaymentsApi;
import com.adyen.service.exception.ApiException;
import com.adyen.util.HMACValidator;
import com.example.payment.adyen.dao.PaymentDao;
import com.example.payment.adyen.dao.PaymentWebhookDao;
import com.example.payment.adyen.dto.PaymentDTO;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.config.AdyenConfig;
import com.example.payment.exceptions.PaymentNotFoundException;
import com.example.payment.helper.PaymentMethodHelper;
import com.example.payment.helper.PaymentStatusEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.security.SignatureException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {

    private PaymentService paymentService;
    private PaymentsApi paymentsApi;
    private AdyenConfig adyenConfig;
    private PaymentDao paymentDao;
    private PaymentWebhookDao paymentWebhookDao;

    @BeforeEach
    void setUp() {
        paymentsApi = mock(PaymentsApi.class);
        adyenConfig = mock(AdyenConfig.class);
        paymentDao = mock(PaymentDao.class);
        paymentWebhookDao = mock(PaymentWebhookDao.class);

        paymentService = new PaymentService(paymentsApi, adyenConfig, paymentDao, paymentWebhookDao);
    }

    @Test
    void testCheckBasicAuthValidSuccess() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcm5hbWU6cGFzc3dvcmQ="); // dXNlcm5hbWU6cGFzc3dvcmQ = "username:password"

        when(adyenConfig.getWebhookUsername()).thenReturn("username");
        when(adyenConfig.getWebhookPassword()).thenReturn("password");

        boolean result = paymentService.checkBasicAuthValid(request);

        assertTrue(result);
    }

    @Test
    void testCheckBasicAuthValidFailure() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic dXNlcm5hbWU6d3JvbmdwYXNzd29yZA=="); // dXNlcm5hbWU6d3JvbmdwYXNzd29yZA== = "username:wrongpassword"

        when(adyenConfig.getWebhookUsername()).thenReturn("username");
        when(adyenConfig.getWebhookPassword()).thenReturn("password");

        boolean result = paymentService.checkBasicAuthValid(request);

        assertFalse(result);
    }

    @Test
    void testCheckBasicAuthValidNoAuthHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        boolean result = paymentService.checkBasicAuthValid(request);

        assertFalse(result);
    }

    @Test
    void testCheckAdyenHMACSuccess() throws SignatureException {
        HMACValidator mockHMACValidator = mock(HMACValidator.class);
        when(mockHMACValidator.validateHMAC(any(NotificationRequestItem.class), eq("mockedHMACKey"))).thenReturn(true);
        paymentService.setHmacValidator(mockHMACValidator);

        NotificationRequest notificationRequest = mock(NotificationRequest.class);
        NotificationRequestItem notificationRequestItem = mock(NotificationRequestItem.class);
        when(notificationRequest.getNotificationItems()).thenReturn(Collections.singletonList(notificationRequestItem));

        when(adyenConfig.getWebhookHMAC()).thenReturn("mockedHMACKey");

        boolean result = paymentService.checkAdyenHMAC(notificationRequest);

        assertTrue(result);
    }

    @Test
    void testCheckAdyenHMACFailure() throws SignatureException {
        HMACValidator mockHMACValidator = mock(HMACValidator.class);
        when(mockHMACValidator.validateHMAC(any(NotificationRequestItem.class), eq("mockedHMACKey"))).thenReturn(false);
        paymentService.setHmacValidator(mockHMACValidator);

        NotificationRequest notificationRequest = mock(NotificationRequest.class);
        NotificationRequestItem notificationItem = mock(NotificationRequestItem.class);
        when(notificationRequest.getNotificationItems()).thenReturn(Collections.singletonList(notificationItem));

        when(adyenConfig.getWebhookHMAC()).thenReturn("mockedHMACKey");

        boolean result = paymentService.checkAdyenHMAC(notificationRequest);

        assertFalse(result);
    }

    @Test
    void testUpdatePaymentSuccess() {
        PaymentDTO payment = new PaymentDTO();
        String pspReference = "psp1234567";
        String authCode = "AUTHORISATION";

        paymentService.updatePaymentSuccess(payment, pspReference, authCode);

        assertEquals(pspReference, payment.getPspReference());
        assertEquals(authCode, payment.getAuthCode());
        assertEquals(PaymentStatusEnum.SUCCESS, payment.getStatus());

        verify(paymentDao, times(1)).updatePspReferenceStatusAndCode(payment);
    }

    @Test
    void testUpdatePaymentFailureWithoutAuthCode() {
        PaymentDTO payment = new PaymentDTO();

        paymentService.updatePaymentFailure(payment, "Some error occurred");

        assertEquals(PaymentStatusEnum.FAILED, payment.getStatus());
        assertEquals("Some error occurred", payment.getFailureMessage());

        verify(paymentDao, times(1)).updateStatusAndSetMessage(payment);
    }

    @Test
    void testCreatePaymentSuccess() throws PaymentNotFoundException {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setAmount("1000");
        dto.setCurrency("EUR");
        dto.setReferenceNumber("REF111");

        when(adyenConfig.getMerchantAccount()).thenReturn("MyMerchant");

        PaymentDTO insertedPayment = new PaymentDTO();
        when(paymentDao.insert(any(PaymentDTO.class))).thenReturn(Optional.of(insertedPayment));

        PaymentDTO result = paymentService.createPayment(dto, "IDEAL");

        assertNotNull(result);
        assertEquals(insertedPayment, result);
        verify(paymentDao, times(1)).insert(any(PaymentDTO.class));
    }

    @Test
    void testCreatePaymentThrowException() {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setAmount("1000");
        dto.setCurrency("EUR");
        dto.setReferenceNumber("REF111");

        when(adyenConfig.getMerchantAccount()).thenReturn("MyMerchant");
        when(paymentDao.insert(any(PaymentDTO.class))).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.createPayment(dto, "SCHEME");
        });
    }

    @Test
    void testMakePaymentSuccess() throws IOException, ApiException {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setId(1L);

        String amount = "1000";
        String currency = "EUR";
        String referenceNumber = "REF123";
        String returnUrl = "https://return.url";

        Object paymentDetails = new Object();

        CheckoutPaymentMethod mockPaymentMethod = mock(CheckoutPaymentMethod.class);
        mockStatic(PaymentMethodHelper.class);
        when(PaymentMethodHelper.createCheckoutPaymentMethod(paymentDetails)).thenReturn(mockPaymentMethod);

        when(adyenConfig.getMerchantAccount()).thenReturn("TestMerchant");

        PaymentResponse mockResponse = new PaymentResponse();
        when(paymentsApi.payments(any(PaymentRequest.class), any(RequestOptions.class))).thenReturn(mockResponse);

        PaymentResponse result = paymentService.makePayment(paymentDTO, paymentDetails, amount, currency, referenceNumber, returnUrl);

        assertNotNull(result);
        verify(paymentsApi).payments(any(PaymentRequest.class), any(RequestOptions.class));
    }

    @Test
    void testMakePayment_ApiExceptionThrown() throws Exception {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setId(1L);

        Object paymentDetails = new Object();
        String amount = "1000";
        String currency = "EUR";
        String referenceNumber = "REF123";
        String returnUrl = "https://return.url";

//        mockStatic(PaymentMethodHelper.class);
        when(PaymentMethodHelper.createCheckoutPaymentMethod(paymentDetails)).thenReturn(mock(CheckoutPaymentMethod.class));
        when(adyenConfig.getMerchantAccount()).thenReturn("TestMerchant");

        when(paymentsApi.payments(any(PaymentRequest.class), any(RequestOptions.class)))
                .thenThrow(new ApiException("Adyen API error", 500, Map.of()));

        assertThrows(ApiException.class, () ->
                paymentService.makePayment(paymentDTO, paymentDetails, amount, currency, referenceNumber, returnUrl));
    }

    @Test
    void testMakePayment_InvalidAmount_ThrowsNumberFormatException() {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setId(1L);

        Object paymentDetails = new Object();
        String amount = "abc"; // Invalid amount
        String currency = "EUR";
        String referenceNumber = "REF789";
        String returnUrl = "https://example.com/return";

        assertThrows(NumberFormatException.class, () ->
                paymentService.makePayment(paymentDTO, paymentDetails, amount, currency, referenceNumber, returnUrl));
    }

    @Test
    void testGetPaymentByIdFound() {
        Long paymentId = 1L;
        PaymentDTO expectedPayment = new PaymentDTO();
        expectedPayment.setId(paymentId);

        when(paymentDao.findById(paymentId)).thenReturn(Optional.of(expectedPayment));

        PaymentDTO result = paymentService.getPaymentByID(paymentId);

        assertNotNull(result);
        assertEquals(paymentId, result.getId());

        verify(paymentDao, times(1)).findById(paymentId);
    }

    @Test
    void testGetPaymentByIdNotFound() {
        Long paymentId = 1L;

        when(paymentDao.findById(paymentId)).thenReturn(Optional.empty());

        PaymentDTO result = paymentService.getPaymentByID(paymentId);

        assertNull(result);
        verify(paymentDao, times(1)).findById(paymentId);
    }

    @Test
    void testHandleNotification() {
        NotificationRequestItem item = new NotificationRequestItem();
        item.setEventCode("AUTHORISATION");
        item.setSuccess(true);
        item.setPspReference("psp123");
        item.setMerchantAccountCode("merchantRef");
        item.setAmount(new Amount().currency("EUR").value(5000L));
        item.setEventDate(new Date());

        PaymentDTO payment = new PaymentDTO();
        payment.setPspReference("psp123");
        payment.setMerchantReference("merchantRef");
        payment.setCurrency("EUR");
        payment.setAmount(50.00);

        when(paymentDao.findByPspReference("psp123")).thenReturn(Optional.of(payment));
        doNothing().when(paymentWebhookDao).insert(any());

        paymentService.handleNotification(item);

        assertEquals("AUTHORISED", payment.getAuthCode());
        assertEquals(PaymentStatusEnum.SUCCESS, payment.getStatus());
        verify(paymentDao, times(1)).updateStatusAndAuth(payment);
        verify(paymentWebhookDao, times(1)).insert(any());
    }

    @Test
    void testHandleNotificationPaymentNotFound() {
        NotificationRequestItem item = new NotificationRequestItem();
        item.setEventCode("AUTHORISATION");
        item.setSuccess(true);
        item.setPspReference("nonExistentPspReference");

        when(paymentDao.findByPspReference("nonExistentPspReference")).thenReturn(Optional.empty());

        paymentService.handleNotification(item);

        // Ensure that no further actions were taken
        verify(paymentDao, times(0)).updateStatusAndAuth(any());
        verify(paymentWebhookDao, times(0)).insert(any());
    }

    @Test
    void testHandleNotificationRefundSuccess() {
        NotificationRequestItem item = new NotificationRequestItem();
        item.setEventCode("REFUND");
        item.setSuccess(true);
        item.setPspReference("psp123");
        item.setMerchantAccountCode("merchantRef");
        item.setAmount(new Amount().currency("EUR").value(5000L));
        item.setEventDate(new Date());

        PaymentDTO payment = new PaymentDTO();
        payment.setPspReference("psp123");
        payment.setMerchantReference("merchantRef");
        payment.setCurrency("EUR");
        payment.setAmount(50.00);

        when(paymentDao.findByPspReference("psp123")).thenReturn(Optional.of(payment));
        doNothing().when(paymentWebhookDao).insert(any());

        paymentService.handleNotification(item);

        assertEquals("REFUNDED", payment.getAuthCode());
        assertEquals(PaymentStatusEnum.SUCCESS, payment.getStatus());
        verify(paymentDao, times(1)).updateStatusAndAuth(payment);
        verify(paymentWebhookDao, times(1)).insert(any());
    }

}
