package com.infusi.waflow.plugin.services;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "business")
public class ServicesProperties {

    private String name;
    private String phone;
    private String currency = "GHS";
    private String timezone = "Africa/Accra";
    private List<ServiceConfig> services = new ArrayList<>();

    @Data
    public static class ServiceConfig {
        private String id;
        private String name;
        private String description;
        private BigDecimal price;
        private int duration;
        private String category;
    }
}
