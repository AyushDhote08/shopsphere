package com.shopsphere.exception;

public class PaymentFailedException extends RuntimeException {

    private final String transactionReference;

    public PaymentFailedException(String message) {
        super(message);
        this.transactionReference = null;
    }

    public PaymentFailedException(String message, String transactionReference) {
        super(message);
        this.transactionReference = transactionReference;
    }

    public String getTransactionReference() {
        return transactionReference;
    }
}
