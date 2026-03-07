package com.infusi.waflow.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class PaymentRequest {

    private BigDecimal amount;

    @Builder.Default
    private String currency = "GHS";

    private String phoneNumber;

    @Builder.Default
    private String reference = UUID.randomUUID().toString();

    private String description;
    private String callbackUrl;

    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();
}
