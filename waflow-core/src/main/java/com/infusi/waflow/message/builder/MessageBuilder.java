package com.infusi.waflow.message.builder;

import com.infusi.waflow.message.types.TextMessage;

/**
 * Fluent entry point for building WhatsApp messages.
 *
 * <pre>{@code
 * // Text message
 * TextMessage msg = MessageBuilder.to(phone).text("Hello!");
 *
 * // List message
 * ListMessage list = MessageBuilder.to(phone)
 *     .list()
 *     .header("Services")
 *     .body("Pick one")
 *     .section("Category", s -> s.row("id", "Title", "Desc"))
 *     .build();
 *
 * // Button message
 * ButtonMessage btns = MessageBuilder.to(phone)
 *     .buttons()
 *     .body("Continue?")
 *     .button("yes", "Yes")
 *     .button("no", "No")
 *     .build();
 * }</pre>
 */
public final class MessageBuilder {

    private final String to;

    private MessageBuilder(String to) {
        this.to = to;
    }

    public static MessageBuilder to(String phoneNumber) {
        return new MessageBuilder(phoneNumber);
    }

    public TextMessage text(String body) {
        return new TextMessage(to, body);
    }

    public TextMessage text(String body, boolean previewUrl) {
        return new TextMessage(to, body, previewUrl);
    }

    public ListMessageBuilder list() {
        return new ListMessageBuilder(to);
    }

    public ButtonMessageBuilder buttons() {
        return new ButtonMessageBuilder(to);
    }
}
