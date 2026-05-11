package com.vivaeventos.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "event-service", url = "${event-service.url}")
public interface EventServiceClient {

    @GetMapping("/api/events/{eventId}/ticket-types/{ticketType}/price")
    BigDecimal getTicketPrice(@PathVariable UUID eventId, @PathVariable String ticketType);

    @GetMapping("/api/events/{eventId}/capacity/available")
    int getAvailableCapacity(@PathVariable UUID eventId);
}
