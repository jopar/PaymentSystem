package com.example.payment.helper;

import com.adyen.model.checkout.CardDetails;
import com.adyen.model.checkout.CheckoutPaymentMethod;
import com.adyen.model.checkout.IdealDetails;
import com.adyen.util.Util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class PaymentMethodHelper {
    private PaymentMethodHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isCardPayment(Object details) {
        return details instanceof CardDetails;
    }

    public static boolean isIdealPayment(Object details) {
        return details instanceof IdealDetails;
    }

    public static CardDetails extractCardDetails(Object details) {
        if (details instanceof CardDetails cardDetails) {
            return cardDetails;
        }
        throw new IllegalArgumentException("Provided payment details are not of type CardDetails. Found: " + details.getClass().getSimpleName());
    }

    public static IdealDetails extractIdealDetails(Object details) {
        if (details instanceof IdealDetails idealDetails) {
            return idealDetails;
        }
        throw new IllegalArgumentException("Provided payment details are not of type IdealDetails. Found: " + details.getClass().getSimpleName());
    }

    public static String getTypeFromPaymentMethod(Object details) {
        String paymentType;

        if (details instanceof CardDetails) {
            CardDetails cardDetails = PaymentMethodHelper.extractCardDetails(details);
            paymentType = cardDetails.getType().getValue();
        } else if (details instanceof IdealDetails) {
            IdealDetails idealDetails = PaymentMethodHelper.extractIdealDetails(details);
            paymentType = idealDetails.getType().getValue();
        } else {
            throw new IllegalArgumentException("Unsupported payment method type.");
        }

        return paymentType;
    }

    public static CheckoutPaymentMethod createCheckoutPaymentMethod(Object details) {
        CheckoutPaymentMethod checkoutPaymentMethod;

        if (details instanceof CardDetails) {
            CardDetails cardDetails = PaymentMethodHelper.extractCardDetails(details);
            checkoutPaymentMethod = new CheckoutPaymentMethod(cardDetails);
        } else if (details instanceof IdealDetails) {
            IdealDetails idealDetails = PaymentMethodHelper.extractIdealDetails(details);
            checkoutPaymentMethod = new CheckoutPaymentMethod(idealDetails);
        } else {
            throw new IllegalArgumentException("Unsupported payment method type.");
        }

        return checkoutPaymentMethod;
    }

    public static long toMinorUnits(Double amount, String currencyCode)
    {
        int fractionDigits = Util.getDecimalPlaces(currencyCode);

        BigDecimal amountBD = BigDecimal.valueOf(amount);
        BigDecimal minorUnits = amountBD.movePointRight(fractionDigits);
        return minorUnits.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
