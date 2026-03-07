package com.infusi.waflow.flow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for all available flows. Plugins register their flows here.
 */
@Slf4j
@Component
public class FlowRegistry {

    private final Map<String, Flow> flows = new ConcurrentHashMap<>();

    public void register(Flow flow) {
        flows.put(flow.getId(), flow);
        log.info("Registered flow: {} ({})", flow.getName(), flow.getId());
    }

    public Optional<Flow> getFlow(String flowId) {
        return Optional.ofNullable(flows.get(flowId));
    }

    public List<Flow> getAllFlows() {
        return List.copyOf(flows.values());
    }

    public boolean hasFlow(String flowId) {
        return flows.containsKey(flowId);
    }
}
