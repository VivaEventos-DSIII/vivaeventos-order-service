package com.vivaeventos.orderservice.kafka.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        UUID eventId,
        UUID customerId,
        int quantity,
        String reason,
        LocalDateTime cancelledAt
) {}
