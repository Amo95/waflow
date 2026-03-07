package com.infusi.waflow.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes payment operations to the configured gateway.
 */
@Slf4j
@Service
public class PaymentProcessor {

    private final Map<String, PaymentGateway> gatewayMap;
    private final String defaultGateway;

    public PaymentProcessor(List<PaymentGateway> gateways,
                            @Value("${waflow.payment.default-gateway:momo}") String defaultGateway) {
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(PaymentGateway::getName, Function.identity()));
        this.defaultGateway = defaultGateway;
        log.info("Payment processor initialized with gateways: {}", gatewayMap.keySet());
    }

    public CompletableFuture<PaymentResponse> initiatePayment(PaymentRequest request) {
        return initiatePayment(request, defaultGateway);
    }

    public CompletableFuture<PaymentResponse> initiatePayment(PaymentRequest request, String gateway) {
        PaymentGateway gw = resolveGateway(gateway);
        log.info("Initiating payment via {}: ref={}, amount={}",
                gateway, request.getReference(), request.getAmount());
        return gw.initiate(request);
    }

    public CompletableFuture<PaymentStatus> verifyPayment(String reference) {
        return verifyPayment(reference, defaultGateway);
    }

    public CompletableFuture<PaymentStatus> verifyPayment(String reference, String gateway) {
        return resolveGateway(gateway).verify(reference);
    }

    private PaymentGateway resolveGateway(String name) {
        PaymentGateway gw = gatewayMap.get(name);
        if (gw == null) {
            throw new IllegalArgumentException("Unknown payment gateway: " + name);
        }
        return gw;
    }
}
