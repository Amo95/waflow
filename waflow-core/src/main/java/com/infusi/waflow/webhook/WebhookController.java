package com.infusi.waflow.webhook;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("${waflow.whatsapp.webhook-path:/webhook}")
public class WebhookController {

    private final WebhookVerifier verifier;
    private final MessageDispatcher dispatcher;

    public WebhookController(WebhookVerifier verifier, MessageDispatcher dispatcher) {
        this.verifier = verifier;
        this.dispatcher = dispatcher;
    }

    /**
     * Webhook verification endpoint called by Meta during setup.
     */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        if (verifier.verify(mode, token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /**
     * Receive incoming webhook events from WhatsApp.
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> payload) {
        log.debug("Received webhook payload");
        dispatcher.dispatch(payload);
        return ResponseEntity.ok("OK");
    }
}
