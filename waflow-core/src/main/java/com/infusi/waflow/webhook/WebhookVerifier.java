package com.infusi.waflow.webhook;

import com.infusi.waflow.config.WhatsAppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WebhookVerifier {

    private final WhatsAppProperties properties;

    public WebhookVerifier(WhatsAppProperties properties) {
        this.properties = properties;
    }

    /**
     * Verify the webhook subscription challenge from Meta.
     *
     * @return true if mode is "subscribe" and the token matches
     */
    public boolean verify(String mode, String token) {
        if ("subscribe".equals(mode) && properties.getVerifyToken().equals(token)) {
            log.info("Webhook verified successfully");
            return true;
        }
        log.warn("Webhook verification failed: mode={}, token={}", mode, token);
        return false;
    }
}
