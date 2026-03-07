package com.infusi.waflow.message;

import com.infusi.waflow.message.types.*;
import com.infusi.waflow.message.types.ListMessage.ListRow;
import com.infusi.waflow.message.types.ListMessage.ListSection;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Converts {@link WhatsAppMessage} objects into the JSON payload
 * expected by the WhatsApp Cloud API.
 */
@Component
public class MessageConverter {

    public Map<String, Object> toPayload(WhatsAppMessage message) {
        return switch (message) {
            case TextMessage m -> buildTextPayload(m);
            case ListMessage m -> buildListPayload(m);
            case ButtonMessage m -> buildButtonPayload(m);
        };
    }

    private Map<String, Object> buildTextPayload(TextMessage msg) {
        Map<String, Object> payload = basePayload(msg.getTo(), "text");
        payload.put("text", Map.of(
                "preview_url", msg.isPreviewUrl(),
                "body", msg.getBody()
        ));
        return payload;
    }

    private Map<String, Object> buildListPayload(ListMessage msg) {
        Map<String, Object> payload = basePayload(msg.getTo(), "interactive");

        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "list");

        if (msg.getHeader() != null) {
            interactive.put("header", Map.of("type", "text", "text", msg.getHeader()));
        }
        interactive.put("body", Map.of("text", msg.getBody()));
        if (msg.getFooter() != null) {
            interactive.put("footer", Map.of("text", msg.getFooter()));
        }

        List<Map<String, Object>> sections = new ArrayList<>();
        for (ListSection section : msg.getSections()) {
            Map<String, Object> sec = new LinkedHashMap<>();
            sec.put("title", section.title());
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ListRow row : section.rows()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("id", row.id());
                r.put("title", row.title());
                if (row.description() != null) {
                    r.put("description", row.description());
                }
                rows.add(r);
            }
            sec.put("rows", rows);
            sections.add(sec);
        }

        interactive.put("action", Map.of(
                "button", msg.getButtonText(),
                "sections", sections
        ));

        payload.put("interactive", interactive);
        return payload;
    }

    private Map<String, Object> buildButtonPayload(ButtonMessage msg) {
        Map<String, Object> payload = basePayload(msg.getTo(), "interactive");

        Map<String, Object> interactive = new LinkedHashMap<>();
        interactive.put("type", "button");

        if (msg.getHeader() != null) {
            interactive.put("header", Map.of("type", "text", "text", msg.getHeader()));
        }
        interactive.put("body", Map.of("text", msg.getBody()));
        if (msg.getFooter() != null) {
            interactive.put("footer", Map.of("text", msg.getFooter()));
        }

        List<Map<String, Object>> buttons = new ArrayList<>();
        for (ButtonMessage.ReplyButton btn : msg.getButtons()) {
            buttons.add(Map.of(
                    "type", "reply",
                    "reply", Map.of("id", btn.id(), "title", btn.title())
            ));
        }
        interactive.put("action", Map.of("buttons", buttons));

        payload.put("interactive", interactive);
        return payload;
    }

    private Map<String, Object> basePayload(String to, String type) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", to);
        payload.put("type", type);
        return payload;
    }
}
