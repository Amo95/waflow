package com.infusi.waflow.menu;

import java.util.List;

/**
 * A named collection of menu items.
 *
 * @param id    unique menu identifier
 * @param title display title
 * @param items ordered list of menu items
 */
public record Menu(String id, String title, List<MenuItem> items) {
    public Menu {
        items = List.copyOf(items);
    }
}
