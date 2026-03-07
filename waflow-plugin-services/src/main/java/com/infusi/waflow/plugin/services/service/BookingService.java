package com.infusi.waflow.plugin.services.service;

import com.infusi.waflow.plugin.services.model.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class BookingService {

    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    public Booking createBooking(String customerPhone, String customerName,
                                 String serviceId, LocalDateTime dateTime, String notes) {
        Booking booking = Booking.builder()
                .customerPhone(customerPhone)
                .customerName(customerName)
                .serviceId(serviceId)
                .dateTime(dateTime)
                .notes(notes)
                .build();
        bookings.put(booking.getId(), booking);
        log.info("Booking created: {} for service {} at {}", booking.getId(), serviceId, dateTime);
        return booking;
    }

    public Optional<Booking> getBooking(String bookingId) {
        return Optional.ofNullable(bookings.get(bookingId));
    }

    public List<Booking> getBookingsByPhone(String phone) {
        return bookings.values().stream()
                .filter(b -> b.getCustomerPhone().equals(phone))
                .toList();
    }

    public void confirmBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking != null) {
            booking.setStatus(Booking.BookingStatus.CONFIRMED);
            log.info("Booking confirmed: {}", bookingId);
        }
    }

    public void cancelBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking != null) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            log.info("Booking cancelled: {}", bookingId);
        }
    }
}
