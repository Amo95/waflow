package com.infusi.waflow.plugin.services.flow;

import com.infusi.waflow.flow.*;
import com.infusi.waflow.message.builder.MessageBuilder;
import com.infusi.waflow.message.types.WhatsAppMessage;
import com.infusi.waflow.plugin.services.model.ServiceItem;
import com.infusi.waflow.plugin.services.service.ServiceCatalogService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Flow that displays a formatted price list of all services.
 */
public class PriceListFlow implements Flow {

    private final ServiceCatalogService catalogService;
    private final Map<String, FlowStep> steps;

    public PriceListFlow(ServiceCatalogService catalogService) {
        this.catalogService = catalogService;
        this.steps = Map.of("show_prices", new ShowPricesStep());
    }

    @Override
    public String getId() { return "price_list"; }

    @Override
    public String getName() { return "Price List"; }

    @Override
    public String getInitialStep() { return "show_prices"; }

    @Override
    public FlowStep getStep(String stepId) { return steps.get(stepId); }

    @Override
    public List<FlowStep> getSteps() { return List.copyOf(steps.values()); }

    private class ShowPricesStep implements FlowStep {

        @Override
        public String getId() { return "show_prices"; }

        @Override
        public String getName() { return "Show Prices"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            List<ServiceItem> services = catalogService.getAllServices();
            String currency = catalogService.getCurrency();

            String priceText = services.stream()
                    .collect(Collectors.groupingBy(ServiceItem::getCategory))
                    .entrySet().stream()
                    .map(entry -> {
                        String header = "*" + capitalize(entry.getKey()) + "*";
                        String items = entry.getValue().stream()
                                .map(s -> String.format("  %s - %s %s",
                                        s.getName(), currency, s.getPrice().toPlainString()))
                                .collect(Collectors.joining("\n"));
                        return header + "\n" + items;
                    })
                    .collect(Collectors.joining("\n\n"));

            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .buttons()
                    .header("Price List")
                    .body(priceText)
                    .button("book", "Book Now")
                    .button("back", "Back to Menu")
                    .build();

            return CompletableFuture.completedFuture(FlowResult.sendAndWait(message));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            return null; // Handled via menu
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
