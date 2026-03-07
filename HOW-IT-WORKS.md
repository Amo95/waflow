# WaFlow - How It Works

## Architecture Overview

```
WhatsApp User
    │
    ▼ (sends message)
Meta Cloud API
    │
    ▼ (webhook POST)
WebhookController
    │
    ▼ (parses payload)
MessageDispatcher
    │
    ├─ matches menu item? ──▶ FlowEngine.startFlow()
    │                              │
    └─ no match ────────────▶ FlowEngine.processMessage()
                                   │
                                   ▼
                             SessionManager (Redis)
                             ┌─ gets current flow + step
                             │
                             ▼
                          FlowStep.execute()
                             │
                             ▼
                          FlowResult
                           ├─ SendAndWait → send message, wait for reply
                           ├─ Transition  → jump to another step
                           ├─ SwitchFlow  → jump to another flow
                           ├─ Complete    → clear session
                           └─ Error       → send error message
```

## The Conversation Lifecycle

1. **User sends "Hi"** → Webhook receives it → Dispatcher finds no session → Creates one (flow=`main`, step=`welcome`) → Executes `WelcomeFlow.WelcomeStep` → Sends the main menu list message

2. **User taps "Our Services"** → Dispatcher gets `services` as the list reply ID → `MenuRegistry` maps `services` → `view_services` flow → `FlowEngine.startFlow("view_services")` → Shows categories

3. **User picks "Makeup"** → Session says current flow=`view_services`, step=`show_categories` → `getNextStep("makeup", context)` returns `"show_services"` → Executes `ShowServicesStep` → Lists makeup services

4. **And so on** through each step until the flow completes or the user navigates elsewhere.

---

## How to Build Your Own App

### Step 1: Create a Spring Boot project

Add the starter and a plugin as dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>com.infusi</groupId>
        <artifactId>waflow-spring-boot-starter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.infusi</groupId>
        <artifactId>waflow-plugin-services</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Step 2: Configure `application.yml`

```yaml
waflow:
  whatsapp:
    phone-number-id: ${WHATSAPP_PHONE_NUMBER_ID}  # from Meta dashboard
    access-token: ${WHATSAPP_ACCESS_TOKEN}
    verify-token: my-secret-token                  # you choose this
  plugins:
    services:
      enabled: true

business:
  name: "My Business"
  currency: GHS
  services:
    - id: haircut
      name: "Haircut"
      description: "Fresh cut"
      price: 50.00
      duration: 30
      category: grooming
```

### Step 3: Run it

Point your Meta webhook URL to `https://your-server/webhook`. That's it for the basic case — the services plugin gives you browsing, pricing, and booking out of the box.

---

## Writing a Custom Flow

If you need behavior beyond the plugins, write your own `Flow`:

```java
@Component
public class ContactFlow implements Flow {

    @Override
    public String getId() { return "contact"; }

    @Override
    public String getName() { return "Contact Us"; }

    @Override
    public String getInitialStep() { return "show_contact"; }

    @Override
    public FlowStep getStep(String stepId) { return step; }

    @Override
    public List<FlowStep> getSteps() { return List.of(step); }

    private final FlowStep step = new FlowStep() {

        @Override
        public String getId() { return "show_contact"; }

        @Override
        public String getName() { return "Show Contact Info"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            var msg = MessageBuilder.to(context.getPhoneNumber())
                    .text("Call us: +233 XX XXX XXXX\nEmail: hello@mybiz.com");
            return CompletableFuture.completedFuture(FlowResult.sendAndWait(msg));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            return null; // dead end, user will pick from menu next
        }
    };
}
```

Then register it at startup:

```java
@Component
public class MyAppSetup {

    private final FlowRegistry flowRegistry;
    private final MenuRegistry menuRegistry;

    // constructor injection...

    @PostConstruct
    public void init() {
        flowRegistry.register(new ContactFlow());

        // Map the "contact" menu item to this flow
        menuRegistry.register(new Menu("extra", "Extra", List.of(
            new MenuItem("contact", "Contact Us", "contact")
        )));
    }
}
```

Now when a user taps "Contact Us" from the welcome menu, it routes to your flow.

---

## Key Concepts

| Concept | What it does |
|---|---|
| **Flow** | A named group of steps (like a mini wizard) |
| **FlowStep** | One screen/interaction — sends a message, waits for reply |
| **FlowResult** | What happens after a step runs (send, transition, switch flow, complete) |
| **FlowContext** | Carries data between steps (`context.put("name", value)`) |
| **SessionManager** | Redis — remembers which flow/step each phone number is on |
| **MenuRegistry** | Maps button/list IDs to flow IDs for navigation |
| **MessageBuilder** | Fluent API to build text, list, and button messages |

---

## Running the Example

```bash
cd waflow

# Set JAVA_HOME to Java 21
export JAVA_HOME=/Users/jamesamo/Library/Java/JavaVirtualMachines/corretto-21.0.7/Contents/Home

# Install all modules locally
mvn clean install -DskipTests

# Run the makeup studio example (needs Redis running)
cd waflow-examples/makeup-studio
mvn spring-boot:run
```

The app starts on port 8080. Point your Meta webhook to `https://<your-domain>/webhook` with the verify token you configured, and conversations will flow automatically.

---

## Project Structure

```
waflow/
├── pom.xml                              # Parent POM
├── waflow-core/                         # Core framework (webhook, flow engine, messages, payments)
├── waflow-plugin-services/              # Services plugin (browse, book, price list)
├── waflow-plugin-ecommerce/             # Ecommerce plugin (placeholder)
├── waflow-plugin-fintech/               # Fintech plugin (placeholder)
├── waflow-spring-boot-starter/          # Auto-configuration starter
└── waflow-examples/makeup-studio/       # Working example app
```
