package com.example.payment.adyen.dto;

import com.example.payment.helper.PaymentStatusEnum;

import java.util.Date;

public class PaymentDTO {
    private Long id;
    private String merchantReference;
    private String pspReference;
    private Double amount;
    private String currency;
    private String reference;
    private String paymentMethod;
    private PaymentStatusEnum status;
    private String authCode;
    private String failureMessage;
    private Date createAt;
    private Date updateAt;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMerchantReference() { return merchantReference; }
    public void setMerchantReference(String merchantReference) { this.merchantReference = merchantReference; }

    public String getPspReference() { return pspReference; }
    public void setPspReference(String pspReference) { this.pspReference = pspReference; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatusEnum getStatus() { return status; }
    public void setStatus(PaymentStatusEnum status) { this.status = status; }

    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }

    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }

    public Date getCreateAt() { return createAt; }
    public void setCreateAt(Date createAt) { this.createAt = createAt; }

    public Date getUpdateAt() { return updateAt; }
    public void setUpdateAt(Date updateAt) { this.updateAt = updateAt; }
}
