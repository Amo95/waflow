package com.infusi.waflow.plugin.fintech;

import com.infusi.waflow.flow.FlowRegistry;
import com.infusi.waflow.menu.MenuRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fintech plugin - provides loan application, repayment, and balance flows.
 * To be implemented.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "waflow.plugins.fintech", name = "enabled", havingValue = "true")
public class FintechPlugin {

    private final FlowRegistry flowRegistry;
    private final MenuRegistry menuRegistry;

    public FintechPlugin(FlowRegistry flowRegistry, MenuRegistry menuRegistry) {
        this.flowRegistry = flowRegistry;
        this.menuRegistry = menuRegistry;
    }

    @PostConstruct
    public void initialize() {
        log.info("Fintech plugin initialized (placeholder)");
    }
}
