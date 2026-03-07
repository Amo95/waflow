package com.infusi.waflow.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentEvent {

    private String transactionId;
    private String reference;
    private PaymentStatus status;
    private String gatewayName;
    private String rawPayload;
}
