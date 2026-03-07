package com.infusi.waflow.plugin.services.service;

import com.infusi.waflow.plugin.services.ServicesProperties;
import com.infusi.waflow.plugin.services.model.ServiceCategory;
import com.infusi.waflow.plugin.services.model.ServiceItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServiceCatalogService {

    private final ServicesProperties properties;

    public ServiceCatalogService(ServicesProperties properties) {
        this.properties = properties;
    }

    public List<ServiceCategory> getCategories() {
        return properties.getServices().stream()
                .collect(Collectors.groupingBy(
                        ServicesProperties.ServiceConfig::getCategory,
                        Collectors.counting()))
                .entrySet().stream()
                .map(e -> ServiceCategory.builder()
                        .id(e.getKey())
                        .name(capitalize(e.getKey()))
                        .count(e.getValue().intValue())
                        .build())
                .toList();
    }

    public List<ServiceItem> getServicesByCategory(String category) {
        return properties.getServices().stream()
                .filter(s -> s.getCategory().equalsIgnoreCase(category))
                .map(this::toServiceItem)
                .toList();
    }

    public Optional<ServiceItem> getService(String serviceId) {
        return properties.getServices().stream()
                .filter(s -> s.getId().equalsIgnoreCase(serviceId))
                .map(this::toServiceItem)
                .findFirst();
    }

    public List<ServiceItem> getAllServices() {
        return properties.getServices().stream()
                .map(this::toServiceItem)
                .toList();
    }

    public String getCurrency() {
        return properties.getCurrency();
    }

    private ServiceItem toServiceItem(ServicesProperties.ServiceConfig config) {
        return ServiceItem.builder()
                .id(config.getId())
                .name(config.getName())
                .description(config.getDescription())
                .price(config.getPrice())
                .durationMinutes(config.getDuration())
                .category(config.getCategory())
                .build();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
