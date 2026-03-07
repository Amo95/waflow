package com.infusi.waflow.flow;

import com.infusi.waflow.menu.MenuRegistry;
import com.infusi.waflow.message.MessageSender;
import com.infusi.waflow.message.SendResult;
import com.infusi.waflow.message.types.TextMessage;
import com.infusi.waflow.message.types.WhatsAppMessage;
import com.infusi.waflow.session.SessionData;
import com.infusi.waflow.session.SessionManager;
import com.infusi.waflow.webhook.MessageDispatcher;
import com.infusi.waflow.webhook.dto.IncomingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Full flow execution integration test without Redis.
 * Manually wires FlowRegistry, MenuRegistry, SessionManager (mocked),
 * MessageSender (mocked), FlowEngine, and MessageDispatcher.
 */
class FlowIntegrationTest {

    private static final String PHONE = "233201234567";
    private static final String FLOW_ID = "registration";

    private SessionManager sessionManager;
    private MessageSender messageSender;
    private FlowRegistry flowRegistry;
    private MenuRegistry menuRegistry;
    private FlowEngine flowEngine;
    private MessageDispatcher messageDispatcher;

    private SessionData currentSession;

    @BeforeEach
    void setUp() {
        sessionManager = mock(SessionManager.class);
        messageSender = mock(MessageSender.class);
        flowRegistry = new FlowRegistry();
        menuRegistry = new MenuRegistry();

        flowRegistry.register(buildRegistrationFlow());

        flowEngine = new FlowEngine(sessionManager, messageSender, flowRegistry);
        messageDispatcher = new MessageDispatcher(flowEngine, menuRegistry);

        when(messageSender.send(eq(PHONE), any(WhatsAppMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(new SendResult.Success("wamid.sent")));

        currentSession = SessionData.builder()
                .phoneNumber(PHONE)
                .currentFlow(FLOW_ID)
                .currentStep("ask_name")
                .build();

        when(sessionManager.getOrCreate(PHONE)).thenAnswer(inv -> cloneSession(currentSession));

        doAnswer(inv -> {
            currentSession.setCurrentStep(inv.getArgument(1));
            return null;
        }).when(sessionManager).updateStep(eq(PHONE), any());

        doAnswer(inv -> {
            currentSession.setCurrentFlow(inv.getArgument(1));
            String step = inv.getArgument(2);
            if (step != null) currentSession.setCurrentStep(step);
            return null;
        }).when(sessionManager).switchFlow(eq(PHONE), any(), any());

        doAnswer(inv -> {
            SessionData saved = inv.getArgument(0);
            currentSession.setData(saved.getData());
            return null;
        }).when(sessionManager).save(any(SessionData.class));

        doNothing().when(sessionManager).clear(PHONE);
    }

    @Test
    @DisplayName("3-step flow: AskName -> AskAge -> Complete")
    void shouldExecuteFullFlowFromStartToCompletion() {
        // Step 1: Start flow — should send "What's your name?"
        flowEngine.startFlow(PHONE, FLOW_ID).join();

        ArgumentCaptor<WhatsAppMessage> msgCaptor = ArgumentCaptor.forClass(WhatsAppMessage.class);
        verify(messageSender, times(1)).send(eq(PHONE), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).isInstanceOf(TextMessage.class);
        assertThat(((TextMessage) msgCaptor.getValue()).getBody()).isEqualTo("What's your name?");
        verify(sessionManager).switchFlow(PHONE, FLOW_ID, "ask_name");

        reset(messageSender);
        when(messageSender.send(eq(PHONE), any(WhatsAppMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(new SendResult.Success("wamid.sent2")));

        // Step 2: User sends "James" — should transition to ask_age
        IncomingMessage nameMsg = IncomingMessage.builder()
                .type(IncomingMessage.Type.TEXT)
                .from(PHONE)
                .messageId("wamid.name")
                .content("James")
                .timestamp("1234567891")
                .build();

        flowEngine.processMessage(PHONE, nameMsg).join();

        verify(sessionManager).updateStep(PHONE, "ask_age");
        msgCaptor = ArgumentCaptor.forClass(WhatsAppMessage.class);
        verify(messageSender, times(1)).send(eq(PHONE), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).isInstanceOf(TextMessage.class);
        assertThat(((TextMessage) msgCaptor.getValue()).getBody()).isEqualTo("How old are you?");

        reset(messageSender);
        when(messageSender.send(eq(PHONE), any(WhatsAppMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(new SendResult.Success("wamid.sent3")));

        // Step 3: User sends "25" — should complete and clear session
        IncomingMessage ageMsg = IncomingMessage.builder()
                .type(IncomingMessage.Type.TEXT)
                .from(PHONE)
                .messageId("wamid.age")
                .content("25")
                .timestamp("1234567892")
                .build();

        flowEngine.processMessage(PHONE, ageMsg).join();

        verify(sessionManager).updateStep(PHONE, "complete");
        verify(sessionManager).clear(PHONE);
    }

    @Test
    @DisplayName("MessageDispatcher parses webhook payload and routes to FlowEngine")
    void shouldDispatchTextMessageViaMessageDispatcher() {
        Map<String, Object> payload = Map.of(
                "object", "whatsapp_business_account",
                "entry", List.of(Map.of(
                        "id", "BIZ_ID",
                        "changes", List.of(Map.of(
                                "value", Map.of(
                                        "messaging_product", "whatsapp",
                                        "messages", List.of(Map.of(
                                                "from", PHONE,
                                                "id", "wamid.dispatch",
                                                "timestamp", "1234567890",
                                                "type", "text",
                                                "text", Map.of("body", "James")
                                        ))
                                ),
                                "field", "messages"
                        ))
                ))
        );

        messageDispatcher.dispatch(payload);

        verify(sessionManager).updateStep(PHONE, "ask_age");
        ArgumentCaptor<WhatsAppMessage> msgCaptor = ArgumentCaptor.forClass(WhatsAppMessage.class);
        verify(messageSender).send(eq(PHONE), msgCaptor.capture());
        assertThat(msgCaptor.getValue()).isInstanceOf(TextMessage.class);
        assertThat(((TextMessage) msgCaptor.getValue()).getBody()).isEqualTo("How old are you?");
    }

    // ---- Test Flow: AskName -> AskAge -> Complete ----

    private Flow buildRegistrationFlow() {
        FlowStep askName = new FlowStep() {
            @Override public String getId() { return "ask_name"; }
            @Override public String getName() { return "Ask Name"; }

            @Override
            public CompletableFuture<FlowResult> execute(FlowContext context) {
                return CompletableFuture.completedFuture(
                        FlowResult.sendAndWait(new TextMessage(context.getPhoneNumber(), "What's your name?")));
            }

            @Override
            public String getNextStep(String response, FlowContext context) {
                context.put("name", response);
                return "ask_age";
            }
        };

        FlowStep askAge = new FlowStep() {
            @Override public String getId() { return "ask_age"; }
            @Override public String getName() { return "Ask Age"; }

            @Override
            public CompletableFuture<FlowResult> execute(FlowContext context) {
                return CompletableFuture.completedFuture(
                        FlowResult.sendAndWait(new TextMessage(context.getPhoneNumber(), "How old are you?")));
            }

            @Override
            public String getNextStep(String response, FlowContext context) {
                context.put("age", response);
                return "complete";
            }
        };

        FlowStep complete = new FlowStep() {
            @Override public String getId() { return "complete"; }
            @Override public String getName() { return "Complete"; }

            @Override
            public CompletableFuture<FlowResult> execute(FlowContext context) {
                return CompletableFuture.completedFuture(FlowResult.complete());
            }

            @Override
            public String getNextStep(String response, FlowContext context) {
                return null;
            }
        };

        List<FlowStep> steps = List.of(askName, askAge, complete);
        return new Flow() {
            @Override public String getId() { return FLOW_ID; }
            @Override public String getName() { return "Registration"; }
            @Override public String getInitialStep() { return "ask_name"; }
            @Override public FlowStep getStep(String stepId) {
                return steps.stream().filter(s -> s.getId().equals(stepId)).findFirst().orElse(null);
            }
            @Override public List<FlowStep> getSteps() { return steps; }
        };
    }

    private SessionData cloneSession(SessionData original) {
        return SessionData.builder()
                .id(original.getId())
                .phoneNumber(original.getPhoneNumber())
                .currentFlow(original.getCurrentFlow())
                .currentStep(original.getCurrentStep())
                .data(new HashMap<>(original.getData()))
                .build();
    }
}
