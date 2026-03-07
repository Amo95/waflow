package com.infusi.waflow.webhook;

import com.infusi.waflow.flow.FlowEngine;
import com.infusi.waflow.menu.MenuRegistry;
import com.infusi.waflow.webhook.dto.IncomingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parses raw webhook payloads from Meta and dispatches
 * normalized messages to the {@link FlowEngine}.
 */
@Slf4j
@Component
public class MessageDispatcher {

    private final FlowEngine flowEngine;
    private final MenuRegistry menuRegistry;

    public MessageDispatcher(FlowEngine flowEngine, MenuRegistry menuRegistry) {
        this.flowEngine = flowEngine;
        this.menuRegistry = menuRegistry;
    }

    @Async("waflowTaskExecutor")
    @SuppressWarnings("unchecked")
    public void dispatch(Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entries =
                    (List<Map<String, Object>>) payload.get("entry");
            if (entries == null) return;

            for (Map<String, Object> entry : entries) {
                List<Map<String, Object>> changes =
                        (List<Map<String, Object>>) entry.get("changes");
                if (changes == null) continue;

                for (Map<String, Object> change : changes) {
                    Map<String, Object> value =
                            (Map<String, Object>) change.get("value");
                    if (value == null) continue;

                    List<Map<String, Object>> messages =
                            (List<Map<String, Object>>) value.get("messages");
                    if (messages == null) continue;

                    for (Map<String, Object> msg : messages) {
                        IncomingMessage incoming = parseMessage(msg);
                        if (incoming != null) {
                            handleMessage(incoming);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error dispatching webhook payload", e);
        }
    }

    @SuppressWarnings("unchecked")
    private IncomingMessage parseMessage(Map<String, Object> msg) {
        String from = (String) msg.get("from");
        String messageId = (String) msg.get("id");
        String timestamp = (String) msg.get("timestamp");
        String type = (String) msg.get("type");

        if (from == null || type == null) return null;

        IncomingMessage.IncomingMessageBuilder builder = IncomingMessage.builder()
                .from(from)
                .messageId(messageId)
                .timestamp(timestamp);

        switch (type) {
            case "text" -> {
                Map<String, Object> text = (Map<String, Object>) msg.get("text");
                builder.type(IncomingMessage.Type.TEXT)
                        .content(text != null ? (String) text.get("body") : "");
            }
            case "interactive" -> {
                Map<String, Object> interactive =
                        (Map<String, Object>) msg.get("interactive");
                if (interactive != null) {
                    String interactiveType = (String) interactive.get("type");
                    if ("button_reply".equals(interactiveType)) {
                        Map<String, Object> reply =
                                (Map<String, Object>) interactive.get("button_reply");
                        builder.type(IncomingMessage.Type.BUTTON_REPLY)
                                .content(reply != null ? (String) reply.get("id") : "");
                    } else if ("list_reply".equals(interactiveType)) {
                        Map<String, Object> reply =
                                (Map<String, Object>) interactive.get("list_reply");
                        builder.type(IncomingMessage.Type.LIST_REPLY)
                                .content(reply != null ? (String) reply.get("id") : "");
                    }
                }
            }
            default -> {
                builder.type(IncomingMessage.Type.UNKNOWN).content("");
            }
        }
        return builder.build();
    }

    private void handleMessage(IncomingMessage message) {
        String phoneNumber = message.getFrom();
        log.info("Processing message from {}: type={}, content={}",
                phoneNumber, message.getType(), message.getContent());

        // Check if the message matches a menu command
        Optional<String> flowId = menuRegistry.resolveFlow(message.getContent());
        if (flowId.isPresent()) {
            flowEngine.startFlow(phoneNumber, flowId.get());
        } else {
            flowEngine.processMessage(phoneNumber, message);
        }
    }
}
