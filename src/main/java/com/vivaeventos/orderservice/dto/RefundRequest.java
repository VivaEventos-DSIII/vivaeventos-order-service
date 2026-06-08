package com.vivaeventos.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO que llega en el body del POST /api/orders/{id}/refund
 *
 * Ejemplo de JSON:
 * {
 *   "reason": "EVENTO_CANCELADO"
 * }
 *
 * El email del cliente se obtiene del header X-User-Email inyectado por el gateway.
 */
public record RefundRequest(

        @NotBlank(message = "El motivo de devolución es obligatorio")
        String reason
) {}