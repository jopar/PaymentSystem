package com.example.payment.adyen.validator;

import com.adyen.model.notification.Amount;
import com.adyen.model.notification.NotificationRequestItem;
import com.example.payment.adyen.dto.PaymentDTO;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class WebhookValidator {
    private WebhookValidator() {
        throw new IllegalStateException("Utility class");
    }

    public static List<String> validateBeforeInsert(PaymentDTO paymentDTO, NotificationRequestItem notificationRequestItem) {
        List<String> errors = new ArrayList<>();

        Double amount = paymentDTO.getAmount();
        String currency = paymentDTO.getCurrency();
        String merchantReference = paymentDTO.getMerchantReference();

        Amount amountFromRequest = notificationRequestItem.getAmount();

        if (StringUtils.isBlank(currency) || !currency.equals(amountFromRequest.getCurrency())) {
            errors.add("Currency from payment data and notification request is not equal!");
        }
        if (!amount.equals(amountFromRequest.getDecimalValue().doubleValue())) {
            errors.add("Amount from payment data and notification request is not equal!");
        }

        if (StringUtils.isBlank(merchantReference) || !merchantReference.equals(notificationRequestItem.getMerchantAccountCode())) {
            errors.add("Merchant reference from payment data and notification request is not equal!");
        }

        return errors;
    }
}
