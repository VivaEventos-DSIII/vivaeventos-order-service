package com.vivaeventos.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

/**
 * DTO que llega en el body del POST /api/orders/{id}/refund
 *
 * Ejemplo de JSON:
 * {
 *   "userEmail": "cliente@email.com",
 *   "reason": "EVENTO_CANCELADO"
 * }
 */
public record RefundRequest(

        @NotBlank(message = "El email del cliente es obligatorio")
        @Email(message = "El email debe tener formato válido")
        String userEmail,

        @NotBlank(message = "El motivo de devolución es obligatorio")
        String reason
) {}