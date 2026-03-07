package com.infusi.waflow.webhook.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Normalized representation of an incoming WhatsApp message,
 * regardless of whether it is text, interactive reply, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomingMessage {

    public enum Type {
        TEXT, BUTTON_REPLY, LIST_REPLY, IMAGE, LOCATION, UNKNOWN
    }

    private Type type;
    private String from;
    private String messageId;
    private String content;
    private String timestamp;
}
