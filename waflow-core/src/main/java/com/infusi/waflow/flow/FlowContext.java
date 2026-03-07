package com.infusi.waflow.flow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Carries state through a flow execution.
 */
@Data
@Builder
@AllArgsConstructor
public class FlowContext {

    private final String phoneNumber;
    private final String sessionId;
    private String currentFlow;
    private String currentStep;

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    public void put(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    public <T> Optional<T> getOptional(String key, Class<T> type) {
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
}
