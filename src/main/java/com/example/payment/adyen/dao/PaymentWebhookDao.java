package com.example.payment.adyen.dao;

import com.example.payment.adyen.dto.PaymentWebhookDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;

public class PaymentWebhookDao {
    private final NamedParameterJdbcTemplate jdbc;

    public PaymentWebhookDao(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String INSERT_PAYMENT_WEBHOOK_SQL =
            "INSERT INTO payment_webhook (payment_id, event_code, success, psp_reference, event_date, received_at, raw_notification) " +
                    "VALUES (:paymentId, :eventCode, :success, :pspReference, :eventDate, :receivedAt, :rawNotification)";

    private static final String SELECT_PAYMENT_WEBHOOK_BY_ID_SQL =
            "SELECT * FROM payment_webhook WHERE id = :id";

    private static final String SELECT_ALL_PAYMENt_WEBHOOK_BY_PAYMENT_ID =
            "SELECT * FROM payment_webhook WHERE payment_id = :paymentId";

    private static final String UPDATE_PAYMENT_WEBHOOK_SQL =
            "UPDATE payment_webhook SET event_code = :eventCode, success = :success, psp_reference = :pspReference, " +
                    "event_date = :eventDate, received_at = :receivedAt, raw_notification = :rawNotification WHERE id = :id";


    public void insert(PaymentWebhookDTO paymentWebhookDTO) {
        ZoneId zoneId = ZoneId.of("UTC");
        Timestamp timestampEventDate = Timestamp.from(paymentWebhookDTO.getEventDate().toInstant().atZone(zoneId).toInstant());
        Timestamp timestampReceiveAt = Timestamp.from(paymentWebhookDTO.getReceivedAt().toInstant().atZone(zoneId).toInstant());

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("paymentId", paymentWebhookDTO.getPaymentId())
                .addValue("eventCode", paymentWebhookDTO.getEventCode())
                .addValue("success", paymentWebhookDTO.getSuccess())
                .addValue("pspReference", paymentWebhookDTO.getPspReference())
                .addValue("eventDate", timestampEventDate)
                .addValue("receivedAt", timestampReceiveAt)
                .addValue("rawNotification", paymentWebhookDTO.getRawNotification());

        jdbc.update(INSERT_PAYMENT_WEBHOOK_SQL, params);
    }

    public List<PaymentWebhookDTO> getAllWebhooksByPaymentId(Long paymentId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("paymentId", paymentId);

        return jdbc.query(SELECT_ALL_PAYMENt_WEBHOOK_BY_PAYMENT_ID, params, paymentWebhookRowMapper());
    }

    private RowMapper<PaymentWebhookDTO> paymentWebhookRowMapper() {
        return (rs, rowNum) -> {
            PaymentWebhookDTO dto = new PaymentWebhookDTO();
            dto.setId(rs.getLong("id"));
            dto.setPaymentId(rs.getLong("payment_id"));
            dto.setEventCode(rs.getString("event_code"));
            dto.setPspReference(rs.getString("psp_reference"));
            dto.setSuccess(rs.getBoolean("success"));
            dto.setEventDate(rs.getTimestamp("event_date"));
            dto.setReceivedAt(rs.getTimestamp("received_at"));
            dto.setRawNotification(rs.getString("raw_notification"));
            return dto;
        };
    }
}
