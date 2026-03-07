package com.infusi.waflow.flow;

import com.infusi.waflow.message.types.WhatsAppMessage;

import java.util.Collections;
import java.util.Map;

/**
 * Result of executing a {@link FlowStep}.
 */
public sealed interface FlowResult {

    /** Send a message and immediately proceed to the next step. */
    record SendMessage(WhatsAppMessage message) implements FlowResult {}

    /** Send a message and wait for the user's response. */
    record SendAndWait(WhatsAppMessage message) implements FlowResult {}

    /** Transition to another step within the same flow. */
    record Transition(String nextStep) implements FlowResult {}

    /** Switch to an entirely different flow. */
    record SwitchFlow(String flowId, String step) implements FlowResult {}

    /** Flow completed successfully. */
    record Complete(Map<String, Object> data) implements FlowResult {
        public Complete() {
            this(Collections.emptyMap());
        }
    }

    /** An error occurred. */
    record Error(String message, String code) implements FlowResult {
        public Error(String message) {
            this(message, null);
        }
    }

    // Convenience factory methods
    static SendMessage sendMessage(WhatsAppMessage message) {
        return new SendMessage(message);
    }

    static SendAndWait sendAndWait(WhatsAppMessage message) {
        return new SendAndWait(message);
    }

    static Transition transition(String nextStep) {
        return new Transition(nextStep);
    }

    static SwitchFlow switchFlow(String flowId) {
        return new SwitchFlow(flowId, null);
    }

    static SwitchFlow switchFlow(String flowId, String step) {
        return new SwitchFlow(flowId, step);
    }

    static Complete complete() {
        return new Complete();
    }

    static Error error(String message) {
        return new Error(message);
    }
}
