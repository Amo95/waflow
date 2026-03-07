package com.infusi.waflow.message.builder;

import com.infusi.waflow.message.types.ListMessage;
import com.infusi.waflow.message.types.ListMessage.ListSection;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ListMessageBuilder {

    private final String to;
    private String header;
    private String body = "";
    private String footer;
    private String buttonText = "Select";
    private final List<ListSection> sections = new ArrayList<>();

    ListMessageBuilder(String to) {
        this.to = to;
    }

    public ListMessageBuilder header(String header) {
        this.header = header;
        return this;
    }

    public ListMessageBuilder body(String body) {
        this.body = body;
        return this;
    }

    public ListMessageBuilder footer(String footer) {
        this.footer = footer;
        return this;
    }

    public ListMessageBuilder buttonText(String buttonText) {
        this.buttonText = buttonText;
        return this;
    }

    public ListMessageBuilder section(String title, Consumer<SectionBuilder> configurator) {
        SectionBuilder builder = new SectionBuilder(title);
        configurator.accept(builder);
        sections.add(builder.build());
        return this;
    }

    public ListMessage build() {
        if (sections.isEmpty()) {
            throw new IllegalStateException("ListMessage must have at least one section");
        }
        return new ListMessage(to, header, body, footer, buttonText, sections);
    }
}
