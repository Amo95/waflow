package com.infusi.waflow.plugin.services.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String customerPhone;
    private String customerName;
    private String serviceId;
    private LocalDateTime dateTime;

    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    private String notes;

    public enum BookingStatus {
        PENDING, CONFIRMED, CANCELLED, COMPLETED
    }
}
