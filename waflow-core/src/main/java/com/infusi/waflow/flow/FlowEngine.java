package com.infusi.waflow.flow;

import com.infusi.waflow.message.MessageSender;
import com.infusi.waflow.session.SessionData;
import com.infusi.waflow.session.SessionManager;
import com.infusi.waflow.webhook.dto.IncomingMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Core state machine that processes incoming messages, resolves the
 * current flow/step, executes logic, and handles transitions.
 */
@Slf4j
@Component
public class FlowEngine {

    private final SessionManager sessionManager;
    private final MessageSender messageSender;
    private final FlowRegistry flowRegistry;

    public FlowEngine(SessionManager sessionManager,
                      MessageSender messageSender,
                      FlowRegistry flowRegistry) {
        this.sessionManager = sessionManager;
        this.messageSender = messageSender;
        this.flowRegistry = flowRegistry;
    }

    /**
     * Main entry point: process an incoming user message.
     */
    public CompletableFuture<Void> processMessage(String phoneNumber, IncomingMessage message) {
        SessionData session = sessionManager.getOrCreate(phoneNumber);

        Flow flow = flowRegistry.getFlow(session.getCurrentFlow())
                .orElseThrow(() -> new FlowNotFoundException(
                        "Flow not found: " + session.getCurrentFlow()));

        FlowStep step = flow.getStep(session.getCurrentStep());
        if (step == null) {
            throw new StepNotFoundException(
                    "Step not found: " + session.getCurrentStep());
        }

        FlowContext context = FlowContext.builder()
                .phoneNumber(phoneNumber)
                .sessionId(session.getId())
                .currentFlow(session.getCurrentFlow())
                .currentStep(session.getCurrentStep())
                .data(session.getData())
                .build();

        String nextStepId = step.getNextStep(message.getContent(), context);

        if (nextStepId != null) {
            sessionManager.updateStep(phoneNumber, nextStepId);
            // persist any data the step put into context
            session.setData(context.getData());
            sessionManager.save(session);

            FlowStep nextStep = flow.getStep(nextStepId);
            if (nextStep == null) {
                throw new StepNotFoundException("Step not found: " + nextStepId);
            }
            context.setCurrentStep(nextStepId);
            return executeStep(nextStep, context);
        }

        // No transition; re-execute current step (e.g. invalid input)
        return executeStep(step, context);
    }

    /**
     * Start a specific flow for a user (e.g. from a menu selection).
     */
    public CompletableFuture<Void> startFlow(String phoneNumber, String flowId) {
        Flow flow = flowRegistry.getFlow(flowId)
                .orElseThrow(() -> new FlowNotFoundException("Flow not found: " + flowId));

        sessionManager.switchFlow(phoneNumber, flowId, flow.getInitialStep());

        FlowContext context = FlowContext.builder()
                .phoneNumber(phoneNumber)
                .sessionId(sessionManager.getOrCreate(phoneNumber).getId())
                .currentFlow(flowId)
                .currentStep(flow.getInitialStep())
                .build();

        FlowStep initialStep = flow.getStep(flow.getInitialStep());
        if (initialStep == null) {
            throw new StepNotFoundException("Initial step not found: " + flow.getInitialStep());
        }
        return executeStep(initialStep, context);
    }

    private CompletableFuture<Void> executeStep(FlowStep step, FlowContext context) {
        return step.execute(context).thenCompose(result -> handleResult(result, context));
    }

    private CompletableFuture<Void> handleResult(FlowResult result, FlowContext context) {
        return switch (result) {
            case FlowResult.SendMessage r -> {
                yield messageSender.send(context.getPhoneNumber(), r.message())
                        .thenAccept(sr -> {});
            }
            case FlowResult.SendAndWait r -> {
                yield messageSender.send(context.getPhoneNumber(), r.message())
                        .thenAccept(sr -> {});
            }
            case FlowResult.Transition r -> {
                sessionManager.updateStep(context.getPhoneNumber(), r.nextStep());
                Flow flow = flowRegistry.getFlow(context.getCurrentFlow())
                        .orElseThrow();
                FlowStep nextStep = flow.getStep(r.nextStep());
                if (nextStep == null) {
                    throw new StepNotFoundException("Step not found: " + r.nextStep());
                }
                context.setCurrentStep(r.nextStep());
                yield executeStep(nextStep, context);
            }
            case FlowResult.SwitchFlow r -> {
                String flowId = r.flowId();
                Flow flow = flowRegistry.getFlow(flowId)
                        .orElseThrow(() -> new FlowNotFoundException("Flow not found: " + flowId));
                String stepId = r.step() != null ? r.step() : flow.getInitialStep();
                sessionManager.switchFlow(context.getPhoneNumber(), flowId, stepId);
                FlowStep nextStep = flow.getStep(stepId);
                if (nextStep == null) {
                    throw new StepNotFoundException("Step not found: " + stepId);
                }
                context.setCurrentFlow(flowId);
                context.setCurrentStep(stepId);
                yield executeStep(nextStep, context);
            }
            case FlowResult.Complete r -> {
                sessionManager.clear(context.getPhoneNumber());
                log.info("Flow completed for {}: {}", context.getPhoneNumber(), r.data());
                yield CompletableFuture.completedFuture(null);
            }
            case FlowResult.Error r -> {
                log.error("Flow error for {}: {} (code={})",
                        context.getPhoneNumber(), r.message(), r.code());
                yield messageSender.sendError(context.getPhoneNumber(), r.message())
                        .thenAccept(sr -> {});
            }
        };
    }
}
