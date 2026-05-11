package com.vivaeventos.orderservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID eventId,
        UUID customerId,
        String ticketType,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        BigDecimal totalAmount,
        LocalDateTime createdAt
) {}
