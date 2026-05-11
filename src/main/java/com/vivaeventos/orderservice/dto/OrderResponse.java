package com.vivaeventos.orderservice.dto;

import com.vivaeventos.orderservice.module.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID eventId,
        UUID customerId,
        String ticketType,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        BigDecimal totalAmount,
        String status,
        String idempotencyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Convierte la entidad Order a este DTO de respuesta
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getEventId(),
                order.getCustomerId(),
                order.getTicketType(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getDiscountPct(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getIdempotencyKey(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}