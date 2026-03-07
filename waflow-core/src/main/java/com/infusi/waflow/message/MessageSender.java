package com.infusi.waflow.message;

import com.infusi.waflow.config.WhatsAppProperties;
import com.infusi.waflow.message.types.TextMessage;
import com.infusi.waflow.message.types.WhatsAppMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class MessageSender {

    private final WebClient webClient;
    private final WhatsAppProperties properties;
    private final MessageConverter messageConverter;

    public MessageSender(WebClient whatsAppWebClient,
                         WhatsAppProperties properties,
                         MessageConverter messageConverter) {
        this.webClient = whatsAppWebClient;
        this.properties = properties;
        this.messageConverter = messageConverter;
    }

    /**
     * Send a message to a WhatsApp user.
     */
    public CompletableFuture<SendResult> send(String to, WhatsAppMessage message) {
        Map<String, Object> payload = messageConverter.toPayload(message);

        return webClient.post()
                .uri("/{phoneNumberId}/messages", properties.getPhoneNumberId())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    @SuppressWarnings("unchecked")
                    var messages = (java.util.List<Map<String, String>>) response.get("messages");
                    String messageId = (messages != null && !messages.isEmpty())
                            ? messages.get(0).get("id")
                            : "unknown";
                    return (SendResult) new SendResult.Success(messageId);
                })
                .onErrorResume(ex -> {
                    log.error("Failed to send message to {}", to, ex);
                    return Mono.just(new SendResult.Failure(ex.getMessage()));
                })
                .toFuture();
    }

    /**
     * Send a simple error notification to the user.
     */
    public CompletableFuture<SendResult> sendError(String to, String errorMessage) {
        log.warn("Sending error message to {}: {}", to, errorMessage);
        return send(to, new TextMessage(to, "Sorry, something went wrong. Please try again."));
    }
}
