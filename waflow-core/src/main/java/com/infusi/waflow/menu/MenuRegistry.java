package com.infusi.waflow.menu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for menus. Maps menu-item IDs to their target flows
 * so the dispatcher can resolve user selections.
 */
@Slf4j
@Component
public class MenuRegistry {

    private final Map<String, Menu> menus = new ConcurrentHashMap<>();
    private final Map<String, String> itemToFlow = new ConcurrentHashMap<>();

    public void register(Menu menu) {
        menus.put(menu.id(), menu);
        for (MenuItem item : menu.items()) {
            itemToFlow.put(item.id(), item.flowId());
        }
        log.info("Registered menu: {} ({} items)", menu.title(), menu.items().size());
    }

    public Optional<Menu> getMenu(String menuId) {
        return Optional.ofNullable(menus.get(menuId));
    }

    /**
     * Resolve a user's input to a flow ID via menu item mapping.
     */
    public Optional<String> resolveFlow(String input) {
        if (input == null) return Optional.empty();
        return Optional.ofNullable(itemToFlow.get(input.trim().toLowerCase()));
    }
}
