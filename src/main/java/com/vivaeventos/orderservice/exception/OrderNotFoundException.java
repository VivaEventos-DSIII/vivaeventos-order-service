package com.vivaeventos.orderservice.exception;

import java.util.UUID;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Orden no encontrada: " + orderId);
    }
}