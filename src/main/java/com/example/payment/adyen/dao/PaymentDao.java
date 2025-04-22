package com.example.payment.adyen.dao;

import com.example.payment.adyen.dto.PaymentDTO;
import com.example.payment.helper.PaymentStatusEnum;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.util.Optional;

public class PaymentDao {

    private final NamedParameterJdbcTemplate jdbc;

    public PaymentDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<PaymentDTO> insert(PaymentDTO dto) {
        String sql = "INSERT INTO payment (merchant_reference, psp_reference, amount, currency, reference, " +
                "payment_method, status, auth_code, failure_message, create_at, update_at) " +
                "VALUES (:merchantReference, :pspReference, :amount, :currency, :reference, :paymentMethod, " +
                ":status, :authCode, :failureMessage, :createAt, :updateAt)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("merchantReference", dto.getMerchantReference())
                .addValue("pspReference", dto.getPspReference())
                .addValue("amount", dto.getAmount())
                .addValue("currency", dto.getCurrency())
                .addValue("reference", dto.getReference())
                .addValue("paymentMethod", dto.getPaymentMethod())
                .addValue("status", dto.getStatus().getValue())
                .addValue("authCode", dto.getAuthCode())
                .addValue("failureMessage", dto.getFailureMessage())
                .addValue("createAt", dto.getCreateAt())
                .addValue("updateAt", dto.getUpdateAt());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbc.update(sql, params, keyHolder, new String[] {"id"});

        Number key = keyHolder.getKey();
        if (key != null) {
            return findById(key.longValue());
        } else {
            return Optional.empty();
        }
    }

    public Optional<PaymentDTO> findById(Long paymentId) {
        String sql = "SELECT * FROM payment WHERE id = :id";
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql,
                    new MapSqlParameterSource("id", paymentId),
                    paymentRowMapper()
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<PaymentDTO> findByReference(String reference) {
        String sql = "SELECT * FROM payment WHERE reference = :reference";
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql,
                    new MapSqlParameterSource("reference", reference),
                    paymentRowMapper()
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public Optional<PaymentDTO> findByPspReference(String pspReference) {
        String sql = "SELECT * FROM payment WHERE psp_reference = :pspReference";
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    sql,
                    new MapSqlParameterSource("pspReference", pspReference),
                    paymentRowMapper()
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public  void updatePspReferenceStatusAndCode(PaymentDTO payment) {
        String sql = "UPDATE payment SET psp_reference = :pspReference, status = :status, auth_code = :authCode, update_at = now() " +
                "WHERE id = :paymentId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", payment.getStatus().getValue())
                .addValue("authCode", payment.getAuthCode())
                .addValue("pspReference", payment.getPspReference())
                .addValue("paymentId", payment.getId());

        jdbc.update(sql, params);
    }

    public void updateStatusAndAuth(PaymentDTO payment) {
        String sql = "UPDATE payment SET status = :status, auth_code = :authCode, update_at = now() " +
                "WHERE id = :paymentId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", payment.getStatus().getValue())
                .addValue("authCode", payment.getAuthCode())
                .addValue("paymentId", payment.getId());

        jdbc.update(sql, params);
    }

    public void updateStatusAndSetMessage(PaymentDTO payment) {
        String sql = "UPDATE payment SET status = :status, failure_message = :failureMessage, update_at = now() " +
                "WHERE id = :paymentId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", payment.getStatus().getValue())
                .addValue("failureMessage", payment.getFailureMessage())
                .addValue("paymentId", payment.getId());

        jdbc.update(sql, params);
    }

    public void updateStatusAuthCodeAndSetMessage(PaymentDTO payment) {
        String sql = "UPDATE payment SET status = :status, auth_code = :authCode, failure_message = :failureMessage, update_at = now() " +
                "WHERE id = :paymentId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", payment.getStatus().getValue())
                .addValue("authCode", payment.getAuthCode())
                .addValue("failureMessage", payment.getFailureMessage())
                .addValue("paymentId", payment.getId());

        jdbc.update(sql, params);
    }

    public void updateStatus(PaymentDTO payment) {
        String sql = "UPDATE payment SET status = :status, update_at = now() WHERE id = :paymentId";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("status", payment.getStatus().getValue())
                .addValue("paymentId", payment.getId());

        jdbc.update(sql, params);
    }

    private RowMapper<PaymentDTO> paymentRowMapper() {
        return (rs, rowNum) -> {
            PaymentDTO dto = new PaymentDTO();
            dto.setId(rs.getLong("id"));
            dto.setMerchantReference(rs.getString("merchant_reference"));
            dto.setPspReference(rs.getString("psp_reference"));
            dto.setAmount(rs.getDouble("amount"));
            dto.setCurrency(rs.getString("currency"));
            dto.setReference(rs.getString("reference"));
            dto.setPaymentMethod(rs.getString("payment_method"));
            dto.setStatus(PaymentStatusEnum.fromValue(rs.getString("status")));
            dto.setAuthCode(rs.getString("auth_code"));
            dto.setFailureMessage(rs.getString("failure_message"));
            dto.setCreateAt(rs.getTimestamp("create_at"));
            dto.setUpdateAt(rs.getTimestamp("update_at"));
            return dto;
        };
    }
}
