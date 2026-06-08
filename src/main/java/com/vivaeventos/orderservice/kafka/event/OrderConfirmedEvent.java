package com.vivaeventos.orderservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        UUID userId,
        String userEmail,
        String userName,
        BigDecimal amount,
        UUID eventId,
        String ticketType,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        LocalDateTime confirmedAt
) {}
