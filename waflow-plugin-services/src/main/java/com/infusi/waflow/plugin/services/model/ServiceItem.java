package com.infusi.waflow.plugin.services.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceItem {

    private String id;
    private String name;
    private String description;
    private BigDecimal price;
    private int durationMinutes;
    private String category;
}
