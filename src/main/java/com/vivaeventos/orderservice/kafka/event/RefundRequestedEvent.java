package com.vivaeventos.orderservice.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento publicado en Kafka cuando se registra una solicitud de devolución.
 * Topic: order.refund-requested
 *
 * El notification-service consume este evento y envía el email de confirmación
 * al cliente (segundo criterio de la historia de usuario).
 *
 * Incluye totalAmount para que el email muestre el monto a devolver.
 * Incluye userEmail para que notification-service sepa a quién enviar.
 */
public record RefundRequestedEvent(
        UUID orderId,
        UUID eventId,
        UUID customerId,
        String userEmail,
        BigDecimal totalAmount,
        String reason,
        LocalDateTime requestedAt
) {}