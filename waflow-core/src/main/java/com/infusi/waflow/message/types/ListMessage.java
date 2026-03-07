package com.infusi.waflow.message.types;

import lombok.Getter;

import java.util.List;

@Getter
public final class ListMessage extends WhatsAppMessage {

    private final String header;
    private final String body;
    private final String footer;
    private final String buttonText;
    private final List<ListSection> sections;

    public ListMessage(String to, String header, String body, String footer,
                       String buttonText, List<ListSection> sections) {
        super(to);
        this.header = header;
        this.body = body;
        this.footer = footer;
        this.buttonText = buttonText;
        this.sections = List.copyOf(sections);
    }

    public record ListSection(String title, List<ListRow> rows) {
        public ListSection {
            rows = List.copyOf(rows);
        }
    }

    public record ListRow(String id, String title, String description) {
        public ListRow(String id, String title) {
            this(id, title, null);
        }
    }
}
