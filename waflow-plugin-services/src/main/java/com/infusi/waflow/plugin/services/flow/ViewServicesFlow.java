package com.infusi.waflow.plugin.services.flow;

import com.infusi.waflow.flow.*;
import com.infusi.waflow.message.builder.MessageBuilder;
import com.infusi.waflow.message.types.WhatsAppMessage;
import com.infusi.waflow.plugin.services.model.ServiceCategory;
import com.infusi.waflow.plugin.services.model.ServiceItem;
import com.infusi.waflow.plugin.services.service.ServiceCatalogService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Flow for browsing the service catalog by category.
 */
public class ViewServicesFlow implements Flow {

    private final ServiceCatalogService catalogService;
    private final Map<String, FlowStep> steps;

    public ViewServicesFlow(ServiceCatalogService catalogService) {
        this.catalogService = catalogService;
        this.steps = Map.of(
                "show_categories", new ShowCategoriesStep(),
                "show_services", new ShowServicesStep(),
                "show_details", new ShowDetailsStep()
        );
    }

    @Override
    public String getId() { return "view_services"; }

    @Override
    public String getName() { return "View Services"; }

    @Override
    public String getInitialStep() { return "show_categories"; }

    @Override
    public FlowStep getStep(String stepId) { return steps.get(stepId); }

    @Override
    public List<FlowStep> getSteps() { return List.copyOf(steps.values()); }

    // --- Steps ---

    private class ShowCategoriesStep implements FlowStep {

        @Override
        public String getId() { return "show_categories"; }

        @Override
        public String getName() { return "Show Categories"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            List<ServiceCategory> categories = catalogService.getCategories();

            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .list()
                    .header("Our Services")
                    .body("Select a category to view available services.")
                    .buttonText("View")
                    .section("Categories", section -> {
                        for (ServiceCategory cat : categories) {
                            section.row(cat.getId(), cat.getName(),
                                    cat.getCount() + " services available");
                        }
                    })
                    .build();

            return CompletableFuture.completedFuture(FlowResult.sendAndWait(message));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            context.put("selectedCategory", response);
            return "show_services";
        }
    }

    private class ShowServicesStep implements FlowStep {

        @Override
        public String getId() { return "show_services"; }

        @Override
        public String getName() { return "Show Services"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            String category = context.getString("selectedCategory");
            List<ServiceItem> services = catalogService.getServicesByCategory(category);
            String currency = catalogService.getCurrency();

            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .list()
                    .header(capitalize(category) + " Services")
                    .body("Select a service for more details.")
                    .buttonText("Details")
                    .section("Services", section -> {
                        for (ServiceItem svc : services) {
                            section.row(svc.getId(), svc.getName(),
                                    currency + " " + svc.getPrice().toPlainString());
                        }
                    })
                    .build();

            return CompletableFuture.completedFuture(FlowResult.sendAndWait(message));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            context.put("selectedService", response);
            return "show_details";
        }
    }

    private class ShowDetailsStep implements FlowStep {

        @Override
        public String getId() { return "show_details"; }

        @Override
        public String getName() { return "Show Service Details"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            String serviceId = context.getString("selectedService");
            String currency = catalogService.getCurrency();

            return catalogService.getService(serviceId)
                    .map(svc -> {
                        String details = String.format(
                                "*%s*\n\n%s\n\nPrice: %s %s\nDuration: %d minutes",
                                svc.getName(), svc.getDescription(),
                                currency, svc.getPrice().toPlainString(),
                                svc.getDurationMinutes());

                        WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                                .buttons()
                                .body(details)
                                .button("book_" + svc.getId(), "Book Now")
                                .button("back", "Back to Menu")
                                .build();

                        return CompletableFuture.completedFuture(
                                (FlowResult) FlowResult.sendAndWait(message));
                    })
                    .orElseGet(() -> CompletableFuture.completedFuture(
                            FlowResult.error("Service not found: " + serviceId)));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            if (response.startsWith("book_")) {
                context.put("bookingService", response.substring(5));
                return null; // Handled by MenuRegistry -> book flow
            }
            return null; // "back" handled by MenuRegistry
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
