package com.vivaeventos.orderservice.module;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que registra cada cambio de estado de una orden.
 *
 * US-18: "Dado que ocurre una compra cuando se registran eventos
 *         entonces el sistema debe guardar cada estado."
 *
 * Ejemplo de registros para una orden:
 *  1. NULL → PENDING           "Orden creada por el cliente"
 *  2. PENDING → PAYMENT_PROC   "Pago iniciado"
 *  3. PAYMENT_PROC → CONFIRMED "Pago confirmado por pasarela"
 *  4. CONFIRMED → REFUND_REQ   "Cliente solicitó devolución"
 */
@Entity
@Table(name = "order_audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderAuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "previous_status")
    private String previousStatus;    // null en la creación inicial

    @Column(name = "new_status", nullable = false)
    private String newStatus;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "triggered_by")
    private String triggeredBy;       // "SYSTEM", "CLIENT", "PAYMENT_GATEWAY"

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT now()")
    private LocalDateTime createdAt;
}