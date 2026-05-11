package com.vivaeventos.orderservice.kafka.event;

import java.util.UUID;

public record PaymentEvent(UUID orderId, String reason) {}
