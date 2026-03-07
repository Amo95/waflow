package com.infusi.waflow.plugin.services.flow;

import com.infusi.waflow.flow.*;
import com.infusi.waflow.message.builder.MessageBuilder;
import com.infusi.waflow.message.types.WhatsAppMessage;
import com.infusi.waflow.plugin.services.ServicesProperties;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Initial welcome flow shown when a user first messages the business.
 */
public class WelcomeFlow implements Flow {

    private final ServicesProperties properties;
    private final Map<String, FlowStep> steps;

    public WelcomeFlow(ServicesProperties properties) {
        this.properties = properties;
        this.steps = Map.of("welcome", new WelcomeStep());
    }

    @Override
    public String getId() { return "main"; }

    @Override
    public String getName() { return "Welcome"; }

    @Override
    public String getInitialStep() { return "welcome"; }

    @Override
    public FlowStep getStep(String stepId) { return steps.get(stepId); }

    @Override
    public List<FlowStep> getSteps() { return List.copyOf(steps.values()); }

    private class WelcomeStep implements FlowStep {

        @Override
        public String getId() { return "welcome"; }

        @Override
        public String getName() { return "Welcome Message"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            String businessName = properties.getName();

            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .list()
                    .header("Welcome to " + businessName)
                    .body("Hi! How can we help you today? Select an option below.")
                    .buttonText("Menu")
                    .section("Options", section -> {
                        section.row("services", "Our Services", "Browse our service catalog");
                        section.row("prices", "Price List", "View our prices");
                        section.row("portfolio", "Portfolio", "See our recent work");
                        section.row("book", "Book Now", "Schedule an appointment");
                        section.row("contact", "Contact Us", "Get in touch");
                    })
                    .build();

            return CompletableFuture.completedFuture(FlowResult.sendAndWait(message));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            // Menu selections are handled by MenuRegistry, not step transitions
            return null;
        }
    }
}
