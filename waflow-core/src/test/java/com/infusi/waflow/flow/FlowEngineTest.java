package com.infusi.waflow.flow;

import com.infusi.waflow.message.MessageSender;
import com.infusi.waflow.message.SendResult;
import com.infusi.waflow.message.types.TextMessage;
import com.infusi.waflow.message.types.WhatsAppMessage;
import com.infusi.waflow.session.SessionData;
import com.infusi.waflow.session.SessionManager;
import com.infusi.waflow.webhook.dto.IncomingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowEngineTest {

    private static final String PHONE = "233201234567";

    @Mock
    private SessionManager sessionManager;

    @Mock
    private MessageSender messageSender;

    private FlowRegistry flowRegistry;
    private FlowEngine engine;

    @BeforeEach
    void setUp() {
        flowRegistry = new FlowRegistry();
        engine = new FlowEngine(sessionManager, messageSender, flowRegistry);

        lenient().when(messageSender.send(eq(PHONE), any(WhatsAppMessage.class)))
                .thenReturn(CompletableFuture.completedFuture(new SendResult.Success("wamid.ok")));
        lenient().when(messageSender.sendError(eq(PHONE), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult.Success("wamid.err")));
    }

    private IncomingMessage textMessage(String content) {
        return IncomingMessage.builder()
                .type(IncomingMessage.Type.TEXT)
                .from(PHONE)
                .messageId("wamid.test")
                .content(content)
                .build();
    }

    private SessionData session(String flowId, String stepId) {
        return SessionData.builder()
                .phoneNumber(PHONE)
                .currentFlow(flowId)
                .currentStep(stepId)
                .data(new HashMap<>())
                .build();
    }

    // --- Simple test flow/step implementations ---

    private FlowStep stepThatSendsAndWaits(String id, String message, String nextStep) {
        return new FlowStep() {
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public CompletableFuture<FlowResult> execute(FlowContext ctx) {
                return CompletableFuture.completedFuture(
                        FlowResult.sendAndWait(new TextMessage(ctx.getPhoneNumber(), message)));
            }
            @Override public String getNextStep(String response, FlowContext ctx) {
                return nextStep;
            }
        };
    }

    private FlowStep stepThatCompletes(String id) {
        return new FlowStep() {
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public CompletableFuture<FlowResult> execute(FlowContext ctx) {
                return CompletableFuture.completedFuture(FlowResult.complete());
            }
            @Override public String getNextStep(String response, FlowContext ctx) { return null; }
        };
    }

    private FlowStep stepThatErrors(String id, String errorMsg) {
        return new FlowStep() {
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public CompletableFuture<FlowResult> execute(FlowContext ctx) {
                return CompletableFuture.completedFuture(FlowResult.error(errorMsg));
            }
            @Override public String getNextStep(String response, FlowContext ctx) { return null; }
        };
    }

    private FlowStep stepThatTransitions(String id, String targetStep) {
        return new FlowStep() {
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public CompletableFuture<FlowResult> execute(FlowContext ctx) {
                return CompletableFuture.completedFuture(FlowResult.transition(targetStep));
            }
            @Override public String getNextStep(String response, FlowContext ctx) { return null; }
        };
    }

    private FlowStep stepThatSwitchesFlow(String id, String targetFlow, String targetStep) {
        return new FlowStep() {
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public CompletableFuture<FlowResult> execute(FlowContext ctx) {
                return CompletableFuture.completedFuture(FlowResult.switchFlow(targetFlow, targetStep));
            }
            @Override public String getNextStep(String response, FlowContext ctx) { return null; }
        };
    }

    private Flow flow(String id, String initialStep, FlowStep... flowSteps) {
        List<FlowStep> stepList = List.of(flowSteps);
        return new Flow() {
            @Override public String getId() { return id; }
            @Override public String getName() { return id; }
            @Override public String getInitialStep() { return initialStep; }
            @Override public FlowStep getStep(String stepId) {
                return stepList.stream().filter(s -> s.getId().equals(stepId)).findFirst().orElse(null);
            }
            @Override public List<FlowStep> getSteps() { return stepList; }
        };
    }

    // --- Tests ---

    @Nested
    @DisplayName("processMessage")
    class ProcessMessageTests {

        @Test
        @DisplayName("transitions to next step and executes it")
        void shouldTransitionAndExecuteNextStep() {
            FlowStep step1 = stepThatSendsAndWaits("step1", "Question?", "step2");
            FlowStep step2 = stepThatSendsAndWaits("step2", "Next question?", null);
            flowRegistry.register(flow("test", "step1", step1, step2));

            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("test", "step1"));

            engine.processMessage(PHONE, textMessage("answer")).join();

            verify(sessionManager).updateStep(PHONE, "step2");
            ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
            verify(messageSender).send(eq(PHONE), captor.capture());
            assertThat(((TextMessage) captor.getValue()).getBody()).isEqualTo("Next question?");
        }

        @Test
        @DisplayName("re-executes current step when getNextStep returns null")
        void shouldReExecuteCurrentStepWhenNoTransition() {
            FlowStep step = new FlowStep() {
                @Override public String getId() { return "loop"; }
                @Override public String getName() { return "loop"; }
                @Override public CompletableFuture<FlowResult> execute(FlowContext ctx) {
                    return CompletableFuture.completedFuture(
                            FlowResult.sendAndWait(new TextMessage(ctx.getPhoneNumber(), "Try again")));
                }
                @Override public String getNextStep(String response, FlowContext ctx) { return null; }
            };

            flowRegistry.register(flow("test", "loop", step));
            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("test", "loop"));

            engine.processMessage(PHONE, textMessage("bad input")).join();

            verify(sessionManager, never()).updateStep(any(), any());
            ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
            verify(messageSender).send(eq(PHONE), captor.capture());
            assertThat(((TextMessage) captor.getValue()).getBody()).isEqualTo("Try again");
        }

        @Test
        @DisplayName("throws when flow not found")
        void shouldThrowWhenFlowNotFound() {
            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("missing", "step1"));

            assertThatThrownBy(() -> engine.processMessage(PHONE, textMessage("hi")))
                    .isInstanceOf(FlowNotFoundException.class);
        }

        @Test
        @DisplayName("throws when step not found")
        void shouldThrowWhenStepNotFound() {
            flowRegistry.register(flow("test", "step1"));
            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("test", "missing"));

            assertThatThrownBy(() -> engine.processMessage(PHONE, textMessage("hi")))
                    .isInstanceOf(StepNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("startFlow")
    class StartFlowTests {

        @Test
        @DisplayName("switches session and executes initial step")
        void shouldSwitchSessionAndExecuteInitialStep() {
            FlowStep initial = stepThatSendsAndWaits("welcome", "Hi!", null);
            flowRegistry.register(flow("greet", "welcome", initial));

            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("greet", "welcome"));

            engine.startFlow(PHONE, "greet").join();

            verify(sessionManager).switchFlow(PHONE, "greet", "welcome");
            ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
            verify(messageSender).send(eq(PHONE), captor.capture());
            assertThat(((TextMessage) captor.getValue()).getBody()).isEqualTo("Hi!");
        }

        @Test
        @DisplayName("throws when flow not found")
        void shouldThrowForUnknownFlow() {
            assertThatThrownBy(() -> engine.startFlow(PHONE, "nope"))
                    .isInstanceOf(FlowNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("FlowResult handling")
    class FlowResultTests {

        @Test
        @DisplayName("Transition triggers next step execution")
        void shouldHandleTransition() {
            FlowStep step1 = stepThatTransitions("step1", "step2");
            FlowStep step2 = stepThatSendsAndWaits("step2", "Arrived!", null);
            flowRegistry.register(flow("test", "step1", step1, step2));

            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("test", "step1"));

            engine.startFlow(PHONE, "test").join();

            verify(sessionManager).updateStep(PHONE, "step2");
            ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
            verify(messageSender).send(eq(PHONE), captor.capture());
            assertThat(((TextMessage) captor.getValue()).getBody()).isEqualTo("Arrived!");
        }

        @Test
        @DisplayName("SwitchFlow changes to a different flow")
        void shouldHandleSwitchFlow() {
            FlowStep switcher = stepThatSwitchesFlow("start", "other", "greet");
            flowRegistry.register(flow("main", "start", switcher));

            FlowStep greetStep = stepThatSendsAndWaits("greet", "Hello from other flow!", null);
            flowRegistry.register(flow("other", "greet", greetStep));

            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("main", "start"));

            engine.startFlow(PHONE, "main").join();

            verify(sessionManager).switchFlow(PHONE, "other", "greet");
            ArgumentCaptor<WhatsAppMessage> captor = ArgumentCaptor.forClass(WhatsAppMessage.class);
            verify(messageSender).send(eq(PHONE), captor.capture());
            assertThat(((TextMessage) captor.getValue()).getBody()).isEqualTo("Hello from other flow!");
        }

        @Test
        @DisplayName("Complete clears the session")
        void shouldHandleComplete() {
            FlowStep completer = stepThatCompletes("done");
            flowRegistry.register(flow("test", "done", completer));

            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("test", "done"));

            engine.startFlow(PHONE, "test").join();

            verify(sessionManager).clear(PHONE);
            verify(messageSender, never()).send(any(), any());
        }

        @Test
        @DisplayName("Error sends error message to user")
        void shouldHandleError() {
            FlowStep errStep = stepThatErrors("bad", "Something broke");
            flowRegistry.register(flow("test", "bad", errStep));

            when(sessionManager.getOrCreate(PHONE)).thenReturn(session("test", "bad"));

            engine.startFlow(PHONE, "test").join();

            verify(messageSender).sendError(eq(PHONE), eq("Something broke"));
        }
    }
}
