package com.vivaeventos.orderservice.dto;

import com.vivaeventos.orderservice.module.OrderAuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para el historial de auditoría de una orden.
 *
 * Ejemplo de respuesta del endpoint GET /api/orders/{id}/audit:
 * [
 *   {
 *     "id": "...",
 *     "orderId": "...",
 *     "previousStatus": null,
 *     "newStatus": "PENDING",
 *     "description": "Orden creada por el cliente",
 *     "triggeredBy": "CLIENT",
 *     "createdAt": "2026-06-06T14:11:51"
 *   },
 *   {
 *     "previousStatus": "PENDING",
 *     "newStatus": "CONFIRMED",
 *     "description": "Pago confirmado por la pasarela de pagos",
 *     "triggeredBy": "PAYMENT_GATEWAY",
 *     ...
 *   }
 * ]
 */
public record AuditLogResponse(
        UUID id,
        UUID orderId,
        String previousStatus,
        String newStatus,
        String description,
        String triggeredBy,
        LocalDateTime createdAt
) {
        /**
         * Convierte una entidad OrderAuditLog en un AuditLogResponse.
         */
        public static AuditLogResponse from(OrderAuditLog log) {
                return new AuditLogResponse(
                        log.getId(),
                        log.getOrderId(),
                        log.getPreviousStatus(),
                        log.getNewStatus(),
                        log.getDescription(),
                        log.getTriggeredBy(),
                        log.getCreatedAt()
                );
        }
}