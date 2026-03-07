package com.infusi.waflow.plugin.services.flow;

import com.infusi.waflow.flow.*;
import com.infusi.waflow.message.builder.MessageBuilder;
import com.infusi.waflow.message.types.WhatsAppMessage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Flow for viewing the business portfolio / recent work.
 */
public class PortfolioFlow implements Flow {

    private final Map<String, FlowStep> steps;

    public PortfolioFlow() {
        this.steps = Map.of("show_portfolio", new ShowPortfolioStep());
    }

    @Override
    public String getId() { return "portfolio"; }

    @Override
    public String getName() { return "Portfolio"; }

    @Override
    public String getInitialStep() { return "show_portfolio"; }

    @Override
    public FlowStep getStep(String stepId) { return steps.get(stepId); }

    @Override
    public List<FlowStep> getSteps() { return List.copyOf(steps.values()); }

    private static class ShowPortfolioStep implements FlowStep {

        @Override
        public String getId() { return "show_portfolio"; }

        @Override
        public String getName() { return "Show Portfolio"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            // Portfolio content would typically be loaded from a database or CMS.
            // For now, send a placeholder text message.
            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .buttons()
                    .header("Our Portfolio")
                    .body("Check out our recent work! Visit our portfolio page for photos and reviews.\n\nWe update our portfolio regularly with new looks and styles.")
                    .button("book", "Book Now")
                    .button("back", "Back to Menu")
                    .build();

            return CompletableFuture.completedFuture(FlowResult.sendAndWait(message));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            return null; // Handled by menu
        }
    }
}
