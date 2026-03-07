package com.infusi.waflow.message.builder;

import com.infusi.waflow.message.types.ListMessage.ListRow;
import com.infusi.waflow.message.types.ListMessage.ListSection;

import java.util.ArrayList;
import java.util.List;

public class SectionBuilder {

    private final String title;
    private final List<ListRow> rows = new ArrayList<>();

    SectionBuilder(String title) {
        this.title = title;
    }

    public SectionBuilder row(String id, String title) {
        rows.add(new ListRow(id, title));
        return this;
    }

    public SectionBuilder row(String id, String title, String description) {
        rows.add(new ListRow(id, title, description));
        return this;
    }

    ListSection build() {
        return new ListSection(title, rows);
    }
}
