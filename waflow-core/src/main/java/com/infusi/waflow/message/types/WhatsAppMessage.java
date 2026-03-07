package com.infusi.waflow.message.types;

import lombok.Getter;

/**
 * Base class for all outgoing WhatsApp messages.
 */
@Getter
public abstract sealed class WhatsAppMessage
        permits TextMessage, ListMessage, ButtonMessage {

    protected final String to;

    protected WhatsAppMessage(String to) {
        this.to = to;
    }
}
