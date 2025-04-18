package com.example.payment.adyen.dao;

import com.example.payment.adyen.dto.PaymentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PaymentDaoTest {

    private NamedParameterJdbcTemplate jdbcTemplate;
    private PaymentDao paymentDao;

    private PaymentDTO paymentDTO;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        paymentDao = new PaymentDao(jdbcTemplate);

        paymentDTO = new PaymentDTO();
        paymentDTO.setMerchantReference("merchant-123");
        paymentDTO.setPspReference("psp-123");
        paymentDTO.setAmount(1000L);
        paymentDTO.setCurrency("USD");
        paymentDTO.setReference("reference-123");
        paymentDTO.setPaymentMethod("credit_card");
        paymentDTO.setStatus("pending");
        paymentDTO.setAuthCode("auth-123");
        paymentDTO.setFailureMessage("none");
        paymentDTO.setCreateAt(new java.sql.Timestamp(System.currentTimeMillis()));
        paymentDTO.setUpdateAt(new java.sql.Timestamp(System.currentTimeMillis()));
    }

    @Test
    void testInsertPayment() {
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(paymentDTO);

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            KeyHolder keyHolder = (KeyHolder) args[2];
            keyHolder.getKeyList().add(new MapSqlParameterSource("id", 1L).getValues());
            return 1;
        }).when(jdbcTemplate).update(anyString(), any(), any(KeyHolder.class), any(String[].class));

        Optional<PaymentDTO> insertedPayment = paymentDao.insert(paymentDTO);

        assertTrue(insertedPayment.isPresent());
        assertEquals(paymentDTO, insertedPayment.get());
    }

    @Test
    void testFindPaymentById() {
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(paymentDTO);

        Optional<PaymentDTO> result = paymentDao.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(paymentDTO, result.get());
    }

    @Test
    void testFindPaymentByIdNotFound() {
        when(jdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenThrow(new EmptyResultDataAccessException(1));

        Optional<PaymentDTO> result = paymentDao.findById(1L);

        assertFalse(result.isPresent());
    }
}
