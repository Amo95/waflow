package com.infusi.waflow.payment.gateway;

import com.infusi.waflow.payment.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paystack payment gateway integration.
 * Requires waflow.payment.paystack.secret-key to be set.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "waflow.payment.paystack", name = "secret-key")
public class PaystackGateway implements PaymentGateway {

    private final WebClient webClient;

    public PaystackGateway(@Value("${waflow.payment.paystack.secret-key}") String secretKey) {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.paystack.co")
                .defaultHeader("Authorization", "Bearer " + secretKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("Paystack gateway initialized");
    }

    @Override
    public String getName() {
        return "paystack";
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<PaymentResponse> initiate(PaymentRequest request) {
        // Paystack expects amount in kobo/pesewas (smallest unit)
        long amountInPesewas = request.getAmount()
                .multiply(java.math.BigDecimal.valueOf(100))
                .longValue();

        Map<String, Object> body = Map.of(
                "amount", amountInPesewas,
                "currency", request.getCurrency(),
                "reference", request.getReference(),
                "email", request.getMetadata().getOrDefault("email", "customer@waflow.io"),
                "metadata", request.getMetadata()
        );

        return webClient.post()
                .uri("/transaction/initialize")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    String reference = (String) data.get("reference");
                    String authorizationUrl = (String) data.get("authorization_url");
                    return (PaymentResponse) new PaymentResponse.RequiresAction(
                            reference, null, authorizationUrl,
                            "Complete payment at: " + authorizationUrl);
                })
                .onErrorResume(ex -> {
                    log.error("Paystack payment initiation failed", ex);
                    return reactor.core.publisher.Mono.just(
                            new PaymentResponse.Failed(ex.getMessage()));
                })
                .toFuture();
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<PaymentStatus> verify(String reference) {
        return webClient.get()
                .uri("/transaction/verify/{reference}", reference)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Map<String, Object> data = (Map<String, Object>) response.get("data");
                    String status = (String) data.get("status");
                    return switch (status) {
                        case "success" -> PaymentStatus.SUCCESS;
                        case "failed" -> PaymentStatus.FAILED;
                        case "abandoned" -> PaymentStatus.CANCELLED;
                        default -> PaymentStatus.PENDING;
                    };
                })
                .onErrorReturn(PaymentStatus.FAILED)
                .toFuture();
    }

    @Override
    public PaymentEvent handleWebhook(String payload, Map<String, String> headers) {
        log.info("Received Paystack webhook callback");
        return PaymentEvent.builder()
                .gatewayName(getName())
                .rawPayload(payload)
                .build();
    }
}
