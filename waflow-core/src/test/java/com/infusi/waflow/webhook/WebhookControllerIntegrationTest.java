package com.infusi.waflow.webhook;

import com.infusi.waflow.config.WhatsAppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WebhookController.class)
@TestPropertySource(properties = {
        "waflow.whatsapp.webhook-path=/webhook",
        "waflow.whatsapp.phone-number-id=123456",
        "waflow.whatsapp.access-token=test-access-token",
        "waflow.whatsapp.verify-token=test-verify-token"
})
class WebhookControllerIntegrationTest {

    @SpringBootApplication
    @EnableConfigurationProperties(WhatsAppProperties.class)
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WebhookVerifier webhookVerifier;

    @MockitoBean
    private MessageDispatcher messageDispatcher;

    @Nested
    @DisplayName("GET /webhook - verification")
    class VerificationTests {

        @Test
        @DisplayName("returns 200 and challenge when verify_token is correct")
        void shouldReturnChallengeWhenTokenIsCorrect() throws Exception {
            when(webhookVerifier.verify("subscribe", "test-verify-token")).thenReturn(true);

            mockMvc.perform(get("/webhook")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "test-verify-token")
                            .param("hub.challenge", "challenge_12345"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("challenge_12345"));

            verify(webhookVerifier).verify("subscribe", "test-verify-token");
        }

        @Test
        @DisplayName("returns 403 when verify_token is wrong")
        void shouldReturn403WhenTokenIsWrong() throws Exception {
            when(webhookVerifier.verify("subscribe", "wrong-token")).thenReturn(false);

            mockMvc.perform(get("/webhook")
                            .param("hub.mode", "subscribe")
                            .param("hub.verify_token", "wrong-token")
                            .param("hub.challenge", "challenge_12345"))
                    .andExpect(status().isForbidden());

            verify(webhookVerifier).verify("subscribe", "wrong-token");
        }
    }

    @Nested
    @DisplayName("POST /webhook - incoming messages")
    class IncomingMessageTests {

        @Test
        @DisplayName("returns 200 OK for a valid text message payload")
        void shouldReturn200ForValidTextMessage() throws Exception {
            String payload = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [{
                        "id": "BIZ_ID",
                        "changes": [{
                          "value": {
                            "messaging_product": "whatsapp",
                            "messages": [{
                              "from": "233201234567",
                              "id": "wamid.xxx",
                              "timestamp": "1234567890",
                              "type": "text",
                              "text": { "body": "Hello" }
                            }]
                          },
                          "field": "messages"
                        }]
                      }]
                    }
                    """;

            mockMvc.perform(post("/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));

            verify(messageDispatcher).dispatch(any());
        }

        @Test
        @DisplayName("returns 200 OK for a valid interactive list_reply payload")
        void shouldReturn200ForValidListReply() throws Exception {
            String payload = """
                    {
                      "object": "whatsapp_business_account",
                      "entry": [{
                        "id": "BIZ_ID",
                        "changes": [{
                          "value": {
                            "messaging_product": "whatsapp",
                            "messages": [{
                              "from": "233201234567",
                              "id": "wamid.yyy",
                              "timestamp": "1234567891",
                              "type": "interactive",
                              "interactive": {
                                "type": "list_reply",
                                "list_reply": {
                                  "id": "option_1",
                                  "title": "Option 1",
                                  "description": "First option"
                                }
                              }
                            }]
                          },
                          "field": "messages"
                        }]
                      }]
                    }
                    """;

            mockMvc.perform(post("/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));

            verify(messageDispatcher).dispatch(any());
        }

        @Test
        @DisplayName("returns 200 OK for an empty body (graceful handling)")
        void shouldReturn200ForEmptyBody() throws Exception {
            mockMvc.perform(post("/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("OK"));

            verify(messageDispatcher).dispatch(any());
        }
    }
}
