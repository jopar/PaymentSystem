package com.example.payment.adyen.controller;

import com.adyen.model.checkout.*;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.helper.DatabaseHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.SQLException;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration(locations = "classpath:test-config.xml")
public class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DatabaseHelper databaseHelper;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws SQLException {
        objectMapper = new ObjectMapper();

        databaseHelper.printInfo();
        databaseHelper.cleanDatabase();
    }

    @Test
    void testCreatePayment_returns200OK() throws Exception {

//        String requestJson = """
//                    {
//                        "paymentMethodDetails": {
//                            "type": "scheme",
//                            "encryptedCardNumber": "test_4111111111111111",
//                            "encryptedSecurityCode": "test_737",
//                            "encryptedExpiryYear": "test_2030",
//                            "encryptedExpiryMonth": "test_03"
//                        },
//                        "amount": "500",
//                        "currency": "USD",
//                        "referenceNumber": "ref12345",
//                        "returnURL": "http://example.com/return"
//                    }
//                """;

        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setAmount("500");
        dto.setCurrency("USD");
        dto.setReferenceNumber("ref12345");
        dto.setReturnURL("http://example.com/return");
        CardDetails cardDetails = new CardDetails();
        cardDetails.setEncryptedCardNumber("test_4111111111111111");
        cardDetails.setEncryptedSecurityCode("test_737");
        cardDetails.setEncryptedExpiryYear("test_2030");
        cardDetails.setEncryptedExpiryMonth("test_03");
        dto.setPaymentMethodDetails(cardDetails);

        String requestJson = objectMapper.writeValueAsString(dto);

        mockMvc.perform(post("/api/payments/adyen/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Payment successfully processed")));

        Integer countPayment = databaseHelper.countPayment();
        assertEquals(1, countPayment.intValue());
    }

    @Test
    void testInvalidJsonPayload() throws Exception {
        String invalidJson = "{ this is not valid json }";

        mockMvc.perform(post("/api/payments/adyen/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid request payload")));

        Integer countPayment = databaseHelper.countPayment();
        assertEquals(0, countPayment.intValue());
    }

    @Test
    void testValidationErrors() throws Exception {
        PaymentRequestDTO dto = new PaymentRequestDTO();

        String json = objectMapper.writeValueAsString(dto);

        mockMvc.perform(post("/api/payments/adyen/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(greaterThan(0)));

        Integer countPayment = databaseHelper.countPayment();
        assertEquals(0, countPayment.intValue());
    }
}
