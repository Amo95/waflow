package com.infusi.waflow.message;

import com.infusi.waflow.message.builder.MessageBuilder;
import com.infusi.waflow.message.types.ButtonMessage;
import com.infusi.waflow.message.types.ListMessage;
import com.infusi.waflow.message.types.TextMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageConverterTest {

    private final MessageConverter converter = new MessageConverter();
    private static final String PHONE = "233201234567";

    @Test
    @DisplayName("converts TextMessage to correct API payload")
    @SuppressWarnings("unchecked")
    void shouldConvertTextMessage() {
        TextMessage msg = new TextMessage(PHONE, "Hello World");
        Map<String, Object> payload = converter.toPayload(msg);

        assertThat(payload.get("messaging_product")).isEqualTo("whatsapp");
        assertThat(payload.get("recipient_type")).isEqualTo("individual");
        assertThat(payload.get("to")).isEqualTo(PHONE);
        assertThat(payload.get("type")).isEqualTo("text");

        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        assertThat(text.get("body")).isEqualTo("Hello World");
        assertThat(text.get("preview_url")).isEqualTo(false);
    }

    @Test
    @DisplayName("converts TextMessage with preview URL enabled")
    @SuppressWarnings("unchecked")
    void shouldConvertTextMessageWithPreviewUrl() {
        TextMessage msg = new TextMessage(PHONE, "https://example.com", true);
        Map<String, Object> payload = converter.toPayload(msg);

        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        assertThat(text.get("preview_url")).isEqualTo(true);
    }

    @Test
    @DisplayName("converts ListMessage to correct API payload")
    @SuppressWarnings("unchecked")
    void shouldConvertListMessage() {
        ListMessage msg = MessageBuilder.to(PHONE)
                .list()
                .header("Menu")
                .body("Choose an option")
                .footer("Footer text")
                .buttonText("Options")
                .section("Section 1", s -> {
                    s.row("row1", "Row One", "Description 1");
                    s.row("row2", "Row Two");
                })
                .build();

        Map<String, Object> payload = converter.toPayload(msg);

        assertThat(payload.get("type")).isEqualTo("interactive");

        Map<String, Object> interactive = (Map<String, Object>) payload.get("interactive");
        assertThat(interactive.get("type")).isEqualTo("list");

        // Header
        Map<String, Object> header = (Map<String, Object>) interactive.get("header");
        assertThat(header.get("type")).isEqualTo("text");
        assertThat(header.get("text")).isEqualTo("Menu");

        // Body
        Map<String, Object> body = (Map<String, Object>) interactive.get("body");
        assertThat(body.get("text")).isEqualTo("Choose an option");

        // Footer
        Map<String, Object> footer = (Map<String, Object>) interactive.get("footer");
        assertThat(footer.get("text")).isEqualTo("Footer text");

        // Action
        Map<String, Object> action = (Map<String, Object>) interactive.get("action");
        assertThat(action.get("button")).isEqualTo("Options");

        List<Map<String, Object>> sections = (List<Map<String, Object>>) action.get("sections");
        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).get("title")).isEqualTo("Section 1");

        List<Map<String, Object>> rows = (List<Map<String, Object>>) sections.get(0).get("rows");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("id")).isEqualTo("row1");
        assertThat(rows.get(0).get("title")).isEqualTo("Row One");
        assertThat(rows.get(0).get("description")).isEqualTo("Description 1");
        assertThat(rows.get(1).get("id")).isEqualTo("row2");
        assertThat(rows.get(1)).doesNotContainKey("description");
    }

    @Test
    @DisplayName("converts ListMessage without optional header/footer")
    @SuppressWarnings("unchecked")
    void shouldConvertListWithoutHeaderAndFooter() {
        ListMessage msg = MessageBuilder.to(PHONE)
                .list()
                .body("Pick one")
                .section("Items", s -> s.row("a", "Item A"))
                .build();

        Map<String, Object> payload = converter.toPayload(msg);
        Map<String, Object> interactive = (Map<String, Object>) payload.get("interactive");

        assertThat(interactive).doesNotContainKey("header");
        assertThat(interactive).doesNotContainKey("footer");
    }

    @Test
    @DisplayName("converts ButtonMessage to correct API payload")
    @SuppressWarnings("unchecked")
    void shouldConvertButtonMessage() {
        ButtonMessage msg = MessageBuilder.to(PHONE)
                .buttons()
                .header("Confirm")
                .body("Do you accept?")
                .button("yes", "Yes")
                .button("no", "No")
                .build();

        Map<String, Object> payload = converter.toPayload(msg);

        assertThat(payload.get("type")).isEqualTo("interactive");

        Map<String, Object> interactive = (Map<String, Object>) payload.get("interactive");
        assertThat(interactive.get("type")).isEqualTo("button");

        Map<String, Object> body = (Map<String, Object>) interactive.get("body");
        assertThat(body.get("text")).isEqualTo("Do you accept?");

        Map<String, Object> action = (Map<String, Object>) interactive.get("action");
        List<Map<String, Object>> buttons = (List<Map<String, Object>>) action.get("buttons");
        assertThat(buttons).hasSize(2);
        assertThat(buttons.get(0).get("type")).isEqualTo("reply");

        Map<String, Object> reply0 = (Map<String, Object>) buttons.get(0).get("reply");
        assertThat(reply0.get("id")).isEqualTo("yes");
        assertThat(reply0.get("title")).isEqualTo("Yes");

        Map<String, Object> reply1 = (Map<String, Object>) buttons.get(1).get("reply");
        assertThat(reply1.get("id")).isEqualTo("no");
        assertThat(reply1.get("title")).isEqualTo("No");
    }
}
