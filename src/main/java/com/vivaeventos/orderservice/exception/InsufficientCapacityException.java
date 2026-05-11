package com.vivaeventos.orderservice.exception;

import java.util.UUID;

public class InsufficientCapacityException extends RuntimeException {

    public InsufficientCapacityException(UUID eventId) {
        super("Aforo agotado o insuficiente para el evento: " + eventId);
    }
}