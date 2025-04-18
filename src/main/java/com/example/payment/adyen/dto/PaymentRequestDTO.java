package com.example.payment.adyen.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.adyen.model.checkout.CardDetails;
import com.adyen.model.checkout.IdealDetails;

public class PaymentRequestDTO {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = CardDetails.class, name = "scheme"),
            @JsonSubTypes.Type(value = IdealDetails.class, name = "ideal")
    })
    private Object paymentMethodDetails;
    private String amount;
    private String currency;
    private String referenceNumber;
    private String returnURL;


    public Object getPaymentMethodDetails() {
        return paymentMethodDetails;
    }

    public void setPaymentMethodDetails(Object paymentMethodDetails) {
        this.paymentMethodDetails = paymentMethodDetails;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getReturnURL() {
        return returnURL;
    }

    public void setReturnURL(String returnURL) {
        this.returnURL = returnURL;
    }
}
