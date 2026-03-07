package com.infusi.waflow.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionData {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String phoneNumber;

    @Builder.Default
    private String currentFlow = "main";

    @Builder.Default
    private String currentStep = "welcome";

    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
