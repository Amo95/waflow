package com.infusi.waflow.message.types;

import lombok.Getter;

@Getter
public final class TextMessage extends WhatsAppMessage {

    private final String body;
    private final boolean previewUrl;

    public TextMessage(String to, String body) {
        this(to, body, false);
    }

    public TextMessage(String to, String body, boolean previewUrl) {
        super(to);
        this.body = body;
        this.previewUrl = previewUrl;
    }
}
