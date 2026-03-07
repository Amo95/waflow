package com.infusi.waflow.webhook;

import com.infusi.waflow.config.WhatsAppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookVerifierTest {

    private WebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setVerifyToken("my-secret-token");
        verifier = new WebhookVerifier(properties);
    }

    @Test
    @DisplayName("returns true for valid mode and token")
    void shouldReturnTrueForValidModeAndToken() {
        assertThat(verifier.verify("subscribe", "my-secret-token")).isTrue();
    }

    @Test
    @DisplayName("returns false for wrong token")
    void shouldReturnFalseForWrongToken() {
        assertThat(verifier.verify("subscribe", "wrong-token")).isFalse();
    }

    @Test
    @DisplayName("returns false for wrong mode")
    void shouldReturnFalseForWrongMode() {
        assertThat(verifier.verify("unsubscribe", "my-secret-token")).isFalse();
    }

    @Test
    @DisplayName("returns false for null mode")
    void shouldReturnFalseForNullMode() {
        assertThat(verifier.verify(null, "my-secret-token")).isFalse();
    }

    @Test
    @DisplayName("returns false for null token")
    void shouldReturnFalseForNullToken() {
        assertThat(verifier.verify("subscribe", null)).isFalse();
    }

    @Test
    @DisplayName("returns false when both are null")
    void shouldReturnFalseWhenBothNull() {
        assertThat(verifier.verify(null, null)).isFalse();
    }
}
