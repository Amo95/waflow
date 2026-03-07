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
 * MTN Mobile Money gateway integration.
 * Requires waflow.payment.momo.api-key to be set.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "waflow.payment.momo", name = "api-key")
public class MoMoGateway implements PaymentGateway {

    private final WebClient webClient;
    private final String apiKey;
    private final String environment;

    public MoMoGateway(@Value("${waflow.payment.momo.api-key}") String apiKey,
                       @Value("${waflow.payment.momo.environment:sandbox}") String environment) {
        this.apiKey = apiKey;
        this.environment = environment;

        String baseUrl = "sandbox".equals(environment)
                ? "https://sandbox.momodeveloper.mtn.com"
                : "https://momodeveloper.mtn.com";

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Ocp-Apim-Subscription-Key", apiKey)
                .build();
        log.info("MoMo gateway initialized (env={})", environment);
    }

    @Override
    public String getName() {
        return "momo";
    }

    @Override
    public CompletableFuture<PaymentResponse> initiate(PaymentRequest request) {
        Map<String, Object> body = Map.of(
                "amount", request.getAmount().toPlainString(),
                "currency", request.getCurrency(),
                "externalId", request.getReference(),
                "payer", Map.of(
                        "partyIdType", "MSISDN",
                        "partyId", request.getPhoneNumber()
                ),
                "payerMessage", request.getDescription(),
                "payeeNote", request.getDescription()
        );

        return webClient.post()
                .uri("/collection/v1_0/requesttopay")
                .header("X-Reference-Id", request.getReference())
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .map(response -> (PaymentResponse) new PaymentResponse.Success(
                        request.getReference(), request.getReference()))
                .onErrorResume(ex -> {
                    log.error("MoMo payment initiation failed", ex);
                    return reactor.core.publisher.Mono.just(
                            new PaymentResponse.Failed(ex.getMessage()));
                })
                .toFuture();
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<PaymentStatus> verify(String reference) {
        return webClient.get()
                .uri("/collection/v1_0/requesttopay/{referenceId}", reference)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String status = (String) response.get("status");
                    return switch (status) {
                        case "SUCCESSFUL" -> PaymentStatus.SUCCESS;
                        case "FAILED" -> PaymentStatus.FAILED;
                        case "PENDING" -> PaymentStatus.PENDING;
                        default -> PaymentStatus.PROCESSING;
                    };
                })
                .onErrorReturn(PaymentStatus.FAILED)
                .toFuture();
    }

    @Override
    public PaymentEvent handleWebhook(String payload, Map<String, String> headers) {
        log.info("Received MoMo webhook callback");
        return PaymentEvent.builder()
                .gatewayName(getName())
                .rawPayload(payload)
                .build();
    }
}
