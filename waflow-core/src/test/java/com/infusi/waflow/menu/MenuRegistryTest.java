package com.infusi.waflow.menu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MenuRegistryTest {

    private MenuRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new MenuRegistry();
    }

    @Test
    @DisplayName("resolveFlow returns flow ID for registered menu item")
    void shouldResolveFlowForRegisteredItem() {
        Menu menu = new Menu("main", "Main Menu", List.of(
                new MenuItem("services", "Our Services", "view_services"),
                new MenuItem("book", "Book Now", "book_service")
        ));
        registry.register(menu);

        assertThat(registry.resolveFlow("services")).isPresent().contains("view_services");
        assertThat(registry.resolveFlow("book")).isPresent().contains("book_service");
    }

    @Test
    @DisplayName("resolveFlow returns empty for unknown input")
    void shouldReturnEmptyForUnknownInput() {
        assertThat(registry.resolveFlow("unknown")).isEmpty();
    }

    @Test
    @DisplayName("resolveFlow returns empty for null input")
    void shouldReturnEmptyForNull() {
        assertThat(registry.resolveFlow(null)).isEmpty();
    }

    @Test
    @DisplayName("resolveFlow trims and lowercases input")
    void shouldTrimAndLowercaseInput() {
        registry.register(new Menu("m", "M", List.of(
                new MenuItem("book", "Book", "book_flow")
        )));

        assertThat(registry.resolveFlow("  book  ")).isPresent().contains("book_flow");
    }

    @Test
    @DisplayName("getMenu retrieves registered menu by ID")
    void shouldGetMenuById() {
        Menu menu = new Menu("main", "Main Menu", List.of(
                new MenuItem("a", "A", "flow_a")
        ));
        registry.register(menu);

        assertThat(registry.getMenu("main")).isPresent();
        assertThat(registry.getMenu("main").get().title()).isEqualTo("Main Menu");
    }

    @Test
    @DisplayName("getMenu returns empty for unknown ID")
    void shouldReturnEmptyMenuForUnknownId() {
        assertThat(registry.getMenu("nope")).isEmpty();
    }

    @Test
    @DisplayName("registering multiple menus merges item-to-flow mappings")
    void shouldMergeAcrossMultipleMenus() {
        registry.register(new Menu("m1", "Menu 1", List.of(
                new MenuItem("a", "A", "flow_a")
        )));
        registry.register(new Menu("m2", "Menu 2", List.of(
                new MenuItem("b", "B", "flow_b")
        )));

        assertThat(registry.resolveFlow("a")).isPresent().contains("flow_a");
        assertThat(registry.resolveFlow("b")).isPresent().contains("flow_b");
    }
}
