package com.infusi.waflow.payment;

public sealed interface PaymentResponse {

    record Success(String transactionId, String reference,
                   PaymentStatus status) implements PaymentResponse {
        public Success(String transactionId, String reference) {
            this(transactionId, reference, PaymentStatus.PENDING);
        }
    }

    record RequiresAction(String transactionId, String ussdCode,
                          String paymentUrl, String instructions) implements PaymentResponse {}

    record Failed(String error, String code) implements PaymentResponse {
        public Failed(String error) {
            this(error, null);
        }
    }
}
