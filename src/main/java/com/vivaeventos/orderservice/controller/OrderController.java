package com.vivaeventos.orderservice.controller;

import com.vivaeventos.orderservice.dto.CreateOrderRequest;
import com.vivaeventos.orderservice.dto.EventSalesResponse;
import com.vivaeventos.orderservice.dto.EventStatisticsResponse;
import com.vivaeventos.orderservice.dto.OrderResponse;
import com.vivaeventos.orderservice.dto.RefundRequest;
import com.vivaeventos.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody CreateOrderRequest request) {
        UUID customerId = UUID.nameUUIDFromBytes(userEmail.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createOrder(request, customerId, userEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getOrderById(id));
    }

    /**
     * POST /api/orders/{id}/refund
     *
     * Registra la solicitud de devolución de una orden.
     *
     * Criterio 1: "Dado que el evento fue cancelado cuando el cliente solicita
     *              devolución entonces el sistema debe registrar la solicitud."
     *
     * Ejemplo de llamada:
     * POST http://localhost:8082/api/orders/550e8400-.../refund
     * {
     *   "userEmail": "cliente@email.com",
     *   "reason": "EVENTO_CANCELADO"
     * }
     *
     * Respuestas:
     * - 200 OK → solicitud registrada, orden en estado REFUND_REQUESTED
     * - 404    → orden no encontrada
     * - 400    → orden no está en estado CONFIRMED
     */
    @PostMapping("/{id}/refund")
    public ResponseEntity<OrderResponse> requestRefund(
            @PathVariable UUID id,
            @RequestHeader("X-User-Email") String userEmail,
            @Valid @RequestBody RefundRequest request) {
        OrderResponse response = service.requestRefund(id, request, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/events/{eventId}/sales")
    public ResponseEntity<EventSalesResponse> getEventSales(@PathVariable UUID eventId) {
        return ResponseEntity.ok(service.getEventSales(eventId));
    }

    @GetMapping("/events/{eventId}/statistics")
    public ResponseEntity<EventStatisticsResponse> getEventStatistics(@PathVariable UUID eventId) {
        return ResponseEntity.ok(service.getEventStatistics(eventId));
    }
}