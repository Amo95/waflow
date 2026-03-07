package com.infusi.waflow.message.builder;

import com.infusi.waflow.message.types.ButtonMessage;
import com.infusi.waflow.message.types.ButtonMessage.ReplyButton;

import java.util.ArrayList;
import java.util.List;

public class ButtonMessageBuilder {

    private final String to;
    private String header;
    private String body = "";
    private String footer;
    private final List<ReplyButton> buttons = new ArrayList<>();

    ButtonMessageBuilder(String to) {
        this.to = to;
    }

    public ButtonMessageBuilder header(String header) {
        this.header = header;
        return this;
    }

    public ButtonMessageBuilder body(String body) {
        this.body = body;
        return this;
    }

    public ButtonMessageBuilder footer(String footer) {
        this.footer = footer;
        return this;
    }

    public ButtonMessageBuilder button(String id, String title) {
        if (buttons.size() >= 3) {
            throw new IllegalStateException("WhatsApp allows a maximum of 3 buttons");
        }
        buttons.add(new ReplyButton(id, title));
        return this;
    }

    public ButtonMessage build() {
        if (buttons.isEmpty()) {
            throw new IllegalStateException("ButtonMessage must have at least one button");
        }
        return new ButtonMessage(to, header, body, footer, buttons);
    }
}
