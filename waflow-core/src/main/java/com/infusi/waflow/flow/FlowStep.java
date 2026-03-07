package com.infusi.waflow.flow;

import java.util.concurrent.CompletableFuture;

/**
 * A single step within a conversational flow.
 */
public interface FlowStep {

    String getId();

    String getName();

    /**
     * Execute this step and produce a result (send message, transition, etc.).
     */
    CompletableFuture<FlowResult> execute(FlowContext context);

    /**
     * Determine the next step based on the user's response.
     *
     * @return next step ID, or {@code null} if this step handles its own transitions
     */
    String getNextStep(String response, FlowContext context);
}
