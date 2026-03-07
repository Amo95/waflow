package com.infusi.waflow.message.types;

import lombok.Getter;

import java.util.List;

@Getter
public final class ButtonMessage extends WhatsAppMessage {

    private final String header;
    private final String body;
    private final String footer;
    private final List<ReplyButton> buttons;

    public ButtonMessage(String to, String header, String body, String footer,
                         List<ReplyButton> buttons) {
        super(to);
        this.header = header;
        this.body = body;
        this.footer = footer;
        this.buttons = List.copyOf(buttons);
    }

    public record ReplyButton(String id, String title) {}
}
