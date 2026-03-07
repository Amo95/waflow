package com.infusi.waflow.plugin.ecommerce;

import com.infusi.waflow.flow.FlowRegistry;
import com.infusi.waflow.menu.MenuRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Ecommerce plugin - provides product catalog, cart, and checkout flows.
 * To be implemented.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "waflow.plugins.ecommerce", name = "enabled", havingValue = "true")
public class EcommercePlugin {

    private final FlowRegistry flowRegistry;
    private final MenuRegistry menuRegistry;

    public EcommercePlugin(FlowRegistry flowRegistry, MenuRegistry menuRegistry) {
        this.flowRegistry = flowRegistry;
        this.menuRegistry = menuRegistry;
    }

    @PostConstruct
    public void initialize() {
        log.info("Ecommerce plugin initialized (placeholder)");
    }
}
