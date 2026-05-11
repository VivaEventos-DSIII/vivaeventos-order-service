package com.vivaeventos.orderservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "eventId es obligatorio")
        UUID eventId,

        @NotNull(message = "customerId es obligatorio")
        UUID customerId,

        @NotNull(message = "ticketType es obligatorio")
        String ticketType,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        int quantity,

        // opcional: el cliente puede enviar un código promo
        String promoCode,

        // opcional: para idempotencia en reintentos
        String idempotencyKey
) {}