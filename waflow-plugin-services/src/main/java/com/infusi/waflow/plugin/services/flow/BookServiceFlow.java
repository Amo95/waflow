package com.infusi.waflow.plugin.services.flow;

import com.infusi.waflow.flow.*;
import com.infusi.waflow.message.builder.MessageBuilder;
import com.infusi.waflow.message.types.WhatsAppMessage;
import com.infusi.waflow.plugin.services.model.Booking;
import com.infusi.waflow.plugin.services.model.ServiceItem;
import com.infusi.waflow.plugin.services.service.BookingService;
import com.infusi.waflow.plugin.services.service.ServiceCatalogService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Multi-step flow to book a service appointment.
 * Steps: select service -> enter name -> enter date/time -> confirm
 */
public class BookServiceFlow implements Flow {

    private final ServiceCatalogService catalogService;
    private final BookingService bookingService;
    private final Map<String, FlowStep> steps;

    public BookServiceFlow(ServiceCatalogService catalogService,
                           BookingService bookingService) {
        this.catalogService = catalogService;
        this.bookingService = bookingService;
        Map<String, FlowStep> map = new LinkedHashMap<>();
        map.put("select_service", new SelectServiceStep());
        map.put("enter_name", new EnterNameStep());
        map.put("enter_date", new EnterDateStep());
        map.put("confirm", new ConfirmStep());
        this.steps = Map.copyOf(map);
    }

    @Override
    public String getId() { return "book_service"; }

    @Override
    public String getName() { return "Book a Service"; }

    @Override
    public String getInitialStep() { return "select_service"; }

    @Override
    public FlowStep getStep(String stepId) { return steps.get(stepId); }

    @Override
    public List<FlowStep> getSteps() { return List.copyOf(steps.values()); }

    // --- Steps ---

    private class SelectServiceStep implements FlowStep {
        @Override
        public String getId() { return "select_service"; }

        @Override
        public String getName() { return "Select Service"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            List<ServiceItem> services = catalogService.getAllServices();
            String currency = catalogService.getCurrency();

            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .list()
                    .header("Book a Service")
                    .body("Which service would you like to book?")
                    .buttonText("Select")
                    .section("Available Services", section -> {
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
            context.put("bookingService", response);
            return "enter_name";
        }
    }

    private class EnterNameStep implements FlowStep {
        @Override
        public String getId() { return "enter_name"; }

        @Override
        public String getName() { return "Enter Name"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .text("Please enter your name for the booking:");

            return CompletableFuture.completedFuture(FlowResult.sendAndWait(message));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            context.put("customerName", response.trim());
            return "enter_date";
        }
    }

    private class EnterDateStep implements FlowStep {
        @Override
        public String getId() { return "enter_date"; }

        @Override
        public String getName() { return "Enter Date"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .text("When would you like to book?\nPlease enter date and time (e.g. 2024-03-15 10:00):");

            return CompletableFuture.completedFuture(FlowResult.sendAndWait(message));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                LocalDateTime dateTime = LocalDateTime.parse(response.trim(), formatter);
                context.put("bookingDate", dateTime.toString());
                return "confirm";
            } catch (DateTimeParseException e) {
                // Stay on same step - invalid date
                return null;
            }
        }
    }

    private class ConfirmStep implements FlowStep {
        @Override
        public String getId() { return "confirm"; }

        @Override
        public String getName() { return "Confirm Booking"; }

        @Override
        public CompletableFuture<FlowResult> execute(FlowContext context) {
            String serviceId = context.getString("bookingService");
            String name = context.getString("customerName");
            String dateStr = context.getString("bookingDate");
            String currency = catalogService.getCurrency();

            ServiceItem service = catalogService.getService(serviceId).orElse(null);
            if (service == null) {
                return CompletableFuture.completedFuture(
                        FlowResult.error("Service not found"));
            }

            String summary = String.format(
                    "*Booking Summary*\n\nService: %s\nPrice: %s %s\nName: %s\nDate: %s\nDuration: %d min",
                    service.getName(), currency, service.getPrice().toPlainString(),
                    name, dateStr, service.getDurationMinutes());

            WhatsAppMessage message = MessageBuilder.to(context.getPhoneNumber())
                    .buttons()
                    .body(summary)
                    .button("confirm_booking", "Confirm")
                    .button("cancel_booking", "Cancel")
                    .build();

            return CompletableFuture.completedFuture(FlowResult.sendAndWait(message));
        }

        @Override
        public String getNextStep(String response, FlowContext context) {
            if ("confirm_booking".equals(response)) {
                String serviceId = context.getString("bookingService");
                String name = context.getString("customerName");
                String dateStr = context.getString("bookingDate");

                Booking booking = bookingService.createBooking(
                        context.getPhoneNumber(),
                        name,
                        serviceId,
                        LocalDateTime.parse(dateStr),
                        null);

                context.put("bookingId", booking.getId());
            }
            return null; // Flow completes
        }
    }
}
