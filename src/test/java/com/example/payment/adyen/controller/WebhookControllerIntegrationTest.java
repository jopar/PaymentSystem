package com.example.payment.adyen.controller;

import com.example.payment.adyen.dao.PaymentDao;
import com.example.payment.adyen.dao.PaymentWebhookDao;
import com.example.payment.adyen.dto.PaymentDTO;
import com.example.payment.adyen.dto.PaymentWebhookDTO;
import com.example.payment.adyen.service.PaymentService;
import com.example.payment.config.AdyenConfig;
import com.example.payment.helper.DatabaseHelper;
import com.example.payment.helper.PaymentStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration(locations = "classpath:test-config.xml")
class WebhookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseHelper databaseHelper;

    @Autowired
    private PaymentDao paymentDao;

    @Autowired
    PaymentWebhookDao paymentWebhookDao;

    @Autowired
    private AdyenConfig adyenConfig;

    @SpyBean
    private PaymentService paymentService;

    @BeforeEach
    void setUp() throws SQLException {
        databaseHelper.printInfo();
        databaseHelper.cleanDatabase();
    }

    @Test
    void testWebhookAuthorisationSuccessful() throws Exception {
        PaymentDTO payment = new PaymentDTO();
        payment.setReference("REF123");
        payment.setPspReference("PSP123456");
        payment.setCurrency("EUR");
        payment.setAmount(1000.00);
        payment.setStatus(PaymentStatusEnum.INITIATED);
        payment.setPaymentMethod("ideal");
        payment.setCreateAt(new Date());
        payment.setUpdateAt(new Date());
        payment.setMerchantReference("TestMerchant");

        paymentDao.insert(payment);

        String jsonPayload = """
                {
                  "live": "false",
                  "notificationItems": [
                    {
                      "NotificationRequestItem": {
                        "additionalData": {
                          "hmacSignature": "lEqHAW/V47OEL996uB0tmZMPJcCJaFf6zG/VrZF1v6E="
                        },
                        "amount": {
                          "currency": "EUR",
                          "value": 100000
                        },
                        "eventCode": "AUTHORISATION",
                        "eventDate": "2025-04-17T18:04:17+02:00",
                        "merchantAccountCode": "TestMerchant",
                        "merchantReference": "REF123",
                        "operations": [
                          "CANCEL",
                          "CAPTURE",
                          "REFUND"
                        ],
                        "paymentMethod": "visa",
                        "pspReference": "PSP123456",
                        "reason": "055025:1111:03/2030",
                        "success": "true"
                      }
                    }
                  ]
                }
                """;


        when(paymentService.checkAdyenHMAC(any())).thenReturn(true);

        mockMvc.perform(post("/api/webhook/adyen")
                        .header("Authorization", "Basic " + HttpHeaders.encodeBasicAuth(adyenConfig.getWebhookUsername(), adyenConfig.getWebhookPassword(), Charset.defaultCharset()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isAccepted())
                .andExpect(content().string("\"OK\""));

        Thread.sleep(3000);

        PaymentDTO updatedPayment = paymentDao.findByPspReference("PSP123456").orElseThrow();
        assertEquals(PaymentStatusEnum.SUCCESS, updatedPayment.getStatus());
        assertEquals("AUTHORISED", updatedPayment.getAuthCode());

        List<PaymentWebhookDTO> webhooks = paymentWebhookDao.getAllWebhooksByPaymentId(updatedPayment.getId());
        assertEquals(1, webhooks.size());
        assertEquals("AUTHORISATION", webhooks.get(0).getEventCode());
    }
}
