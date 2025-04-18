package com.example.payment.adyen.dto;

import java.util.Date;

public class PaymentWebhookDTO {
    private Long id;
    private Long paymentId;
    private String eventCode;
    private Boolean success;
    private String pspReference;
    private Date eventDate;
    private Date receivedAt;
    private String rawNotification;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getEventCode() {
        return eventCode;
    }

    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getPspReference() {
        return pspReference;
    }

    public void setPspReference(String pspReference) {
        this.pspReference = pspReference;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public Date getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Date receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getRawNotification() {
        return rawNotification;
    }

    public void setRawNotification(String rawNotification) {
        this.rawNotification = rawNotification;
    }
}
