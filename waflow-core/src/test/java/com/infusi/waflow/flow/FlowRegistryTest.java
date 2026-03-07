package com.infusi.waflow.flow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class FlowRegistryTest {

    private FlowRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new FlowRegistry();
    }

    @Test
    @DisplayName("registers and retrieves a flow by ID")
    void shouldRegisterAndRetrieveFlow() {
        Flow flow = testFlow("greet", "Greeting");
        registry.register(flow);

        assertThat(registry.getFlow("greet")).isPresent().get().isSameAs(flow);
    }

    @Test
    @DisplayName("returns empty Optional for unknown flow ID")
    void shouldReturnEmptyForUnknownId() {
        assertThat(registry.getFlow("nonexistent")).isEmpty();
    }

    @Test
    @DisplayName("hasFlow returns true for registered, false for unknown")
    void shouldCheckExistence() {
        registry.register(testFlow("a", "Flow A"));

        assertThat(registry.hasFlow("a")).isTrue();
        assertThat(registry.hasFlow("b")).isFalse();
    }

    @Test
    @DisplayName("getAllFlows returns all registered flows")
    void shouldReturnAllFlows() {
        registry.register(testFlow("a", "Flow A"));
        registry.register(testFlow("b", "Flow B"));

        assertThat(registry.getAllFlows()).hasSize(2);
    }

    @Test
    @DisplayName("re-registering same ID replaces the flow")
    void shouldReplaceOnReRegister() {
        Flow v1 = testFlow("greet", "V1");
        Flow v2 = testFlow("greet", "V2");

        registry.register(v1);
        registry.register(v2);

        assertThat(registry.getFlow("greet")).isPresent().get().isSameAs(v2);
        assertThat(registry.getAllFlows()).hasSize(1);
    }

    private Flow testFlow(String id, String name) {
        return new Flow() {
            @Override public String getId() { return id; }
            @Override public String getName() { return name; }
            @Override public String getInitialStep() { return "start"; }
            @Override public FlowStep getStep(String stepId) { return null; }
            @Override public List<FlowStep> getSteps() { return List.of(); }
        };
    }
}
