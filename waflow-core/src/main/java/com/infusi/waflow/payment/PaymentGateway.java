package com.infusi.waflow.payment;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for payment gateway integrations (MoMo, Paystack, etc.).
 */
public interface PaymentGateway {

    String getName();

    CompletableFuture<PaymentResponse> initiate(PaymentRequest request);

    CompletableFuture<PaymentStatus> verify(String reference);

    /**
     * Handle incoming payment webhook/callback.
     */
    PaymentEvent handleWebhook(String payload, Map<String, String> headers);
}
