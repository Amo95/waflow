package com.infusi.waflow.plugin.services;

import com.infusi.waflow.flow.FlowRegistry;
import com.infusi.waflow.menu.Menu;
import com.infusi.waflow.menu.MenuItem;
import com.infusi.waflow.menu.MenuRegistry;
import com.infusi.waflow.plugin.services.flow.*;
import com.infusi.waflow.plugin.services.service.BookingService;
import com.infusi.waflow.plugin.services.service.ServiceCatalogService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "waflow.plugins.services", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(ServicesProperties.class)
public class ServicesPlugin {

    private final FlowRegistry flowRegistry;
    private final MenuRegistry menuRegistry;
    private final ServiceCatalogService catalogService;
    private final BookingService bookingService;
    private final ServicesProperties properties;

    public ServicesPlugin(FlowRegistry flowRegistry,
                          MenuRegistry menuRegistry,
                          ServiceCatalogService catalogService,
                          BookingService bookingService,
                          ServicesProperties properties) {
        this.flowRegistry = flowRegistry;
        this.menuRegistry = menuRegistry;
        this.catalogService = catalogService;
        this.bookingService = bookingService;
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        // Register flows
        flowRegistry.register(new WelcomeFlow(properties));
        flowRegistry.register(new ViewServicesFlow(catalogService));
        flowRegistry.register(new PriceListFlow(catalogService));
        flowRegistry.register(new PortfolioFlow());
        flowRegistry.register(new BookServiceFlow(catalogService, bookingService));

        // Register main menu
        menuRegistry.register(new Menu("main", "Main Menu", List.of(
                new MenuItem("services", "Our Services", "view_services"),
                new MenuItem("prices", "Price List", "price_list"),
                new MenuItem("portfolio", "Portfolio", "portfolio"),
                new MenuItem("book", "Book Now", "book_service"),
                new MenuItem("contact", "Contact Us", "main"),
                new MenuItem("back", "Back to Menu", "main")
        )));

        log.info("Services plugin initialized for: {}", properties.getName());
    }
}
