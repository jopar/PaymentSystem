package com.example.payment.helper;

public enum PaymentStatusEnum {
    SUCCESS("Success"),
    PENDING("Pending"),
    FAILED("Failed"),
    INITIATED("Initiated");

    private final String value;

    PaymentStatusEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static PaymentStatusEnum fromValue(String value) {
        for (PaymentStatusEnum status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unexpected value: " + value);
    }
}
