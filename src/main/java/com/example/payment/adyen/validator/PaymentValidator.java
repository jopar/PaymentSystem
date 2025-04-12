package com.example.payment.adyen.validator;

import com.adyen.model.checkout.CardDetails;
import com.adyen.model.checkout.IdealDetails;
import com.example.payment.adyen.dto.PaymentRequestDTO;
import com.example.payment.helper.PaymentMethodHelper;
import jakarta.servlet.http.HttpServletRequest;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class PaymentValidator {
    public List<String> validateOnPay(PaymentRequestDTO paymentRequestDTO) {
        List<String> errors = new ArrayList<>();

        Object paymentMethodDetails = paymentRequestDTO.getPaymentMethodDetails();
        List<String> paymentDetailsErrors;

        if (PaymentMethodHelper.isCardPayment(paymentMethodDetails)) {
            paymentDetailsErrors = validateCardDetails(PaymentMethodHelper.extractCardDetails(paymentMethodDetails));
        } else if (PaymentMethodHelper.isIdealPayment(paymentMethodDetails)) {
            paymentDetailsErrors = validateIdealDetails(PaymentMethodHelper.extractIdealDetails(paymentMethodDetails));
        } else {
            paymentDetailsErrors = new ArrayList<>();
        }

        if (!paymentDetailsErrors.isEmpty()) {
            errors.addAll(paymentDetailsErrors);
        }

        String amountStr = paymentRequestDTO.getAmount();
        if (amountStr != null) {
            try {
                float amount = Float.parseFloat(amountStr);

                if (amount <= 0) {
                    errors.add("An amount must be greater then 0");
                }
            } catch (NumberFormatException e) {
                errors.add("An amount value must be numeric");
            }
        } else {
            errors.add("An amount is required");
        }

        if (paymentRequestDTO.getCurrency() == null || paymentRequestDTO.getCurrency().isEmpty()) {
            errors.add("Currency is required");
        }

        if (paymentRequestDTO.getReferenceNumber() == null || paymentRequestDTO.getReferenceNumber().isEmpty()) {
            errors.add("A reference number is required");
        }

        if (paymentRequestDTO.getReturnURL() == null || paymentRequestDTO.getReturnURL().isEmpty()) {
            errors.add("A return URL is required");
        }

        return errors;
    }

    private List<String> validateCardDetails(CardDetails cardDetails) {
        List<String> errors = new ArrayList<>();

        if (cardDetails == null) {
            errors.add("Card details are required");
            return errors;
        }

        if (cardDetails.getEncryptedCardNumber() == null || cardDetails.getEncryptedCardNumber().isEmpty()) {
            errors.add("Card number is required");
        }

        if (cardDetails.getEncryptedSecurityCode() == null || cardDetails.getEncryptedSecurityCode().isEmpty()) {
            errors.add("Security code is required");
        }

        boolean isOkYear = true;
        boolean isOkMonth = true;

        if (cardDetails.getEncryptedExpiryYear() == null || cardDetails.getEncryptedExpiryYear().isEmpty()) {
            errors.add("Expiry year is required");
            isOkYear = false;
        }

        if (cardDetails.getEncryptedExpiryMonth() == null || cardDetails.getEncryptedExpiryMonth().isEmpty()) {
            errors.add("Expiry month is required");
            isOkMonth = false;
        }

        if (isOkYear && isOkMonth) {
            isValidExpiry(normalizeTestValue(cardDetails.getEncryptedExpiryMonth()), normalizeTestValue(cardDetails.getEncryptedExpiryYear()), errors);
        }

        if (cardDetails.getType() == null) {
            errors.add("Card type is required");
        } else {
            try {
                CardDetails.TypeEnum.fromValue(cardDetails.getType().getValue());
            } catch (IllegalArgumentException e) {
                errors.add("Invalid card type: " + cardDetails.getType());
            }
        }

        return errors;
    }

    private List<String> validateIdealDetails(IdealDetails idealDetails) {
        List<String> errors = new ArrayList<>();

        if (idealDetails == null) {
            errors.add("Ideal details are required");
            return errors;
        }

        return errors;
    }

    private String normalizeTestValue(String value) {
        if (value == null) return null;

        if (value.startsWith("test_")) {
            return value.substring(5);
        }

        return value;
    }

    private boolean isValidExpiry(String monthStr, String yearStr, List<String> errors) {
        try {
            int month = Integer.parseInt(monthStr);
            int year = Integer.parseInt(yearStr);

            if (month < 1 || month > 12) {
                errors.add("Expiry month must be between 1 and 12");
                return false;
            }

            YearMonth now = YearMonth.now();
            YearMonth expiry = YearMonth.of(year, month);

            if (!expiry.isAfter(now)) {
                errors.add("Card expiry date must be in the future");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            errors.add("Expiry month and year must be numeric");
            return false;
        }
    }

    public List<String> validateOnReturn(HttpServletRequest request) {
        List<String> errors = new ArrayList<>();

        String paymentId = request.getParameter("payment_id");
        String redirectResult = request.getParameter("redirectResult");

        if (paymentId == null) {
            errors.add("Parameter payment_id is required");
        }

        if (redirectResult == null) {
            errors.add("Parameter redirectResult is required");
        }

        try {
            Long.parseLong(paymentId);
        } catch (NumberFormatException e) {
            errors.add("Parameter payment_id must be number format.");
        }

        return errors;
    }
}
