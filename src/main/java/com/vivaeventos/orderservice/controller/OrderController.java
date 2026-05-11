package com.vivaeventos.orderservice.controller;

import com.vivaeventos.orderservice.dto.CreateOrderRequest;
import com.vivaeventos.orderservice.dto.OrderResponse;
import com.vivaeventos.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.vivaeventos.orderservice.dto.EventSalesResponse;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    // US-04 criterio 1: crear orden — recibe DTO, nunca la entidad directa
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = service.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // US-04 testabilidad: consultar estado de una orden
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable UUID id) {
        OrderResponse response = service.getOrderById(id);
        return ResponseEntity.ok(response);
    }
    /**
     * GET /api/orders/events/{eventId}/sales
     *
     * Consulta el reporte de ventas de un evento (US-10).
     * Solo accesible para organizadores (el rol se valida en el api-gateway).
     *
     * @PathVariable eventId → UUID del evento a consultar
     *
     * Respuestas posibles:
     * - HTTP 200 con datos   → hay ventas registradas (criterio 1)
     * - HTTP 200 sin ventas  → no hay ventas, mensaje informativo (criterio 2)
     *
     * Ejemplo de llamada:
     * GET http://localhost:8082/api/orders/events/550e8400-e29b-41d4-a716-446655440000/sales
     */
    @GetMapping("/events/{eventId}/sales")
    public ResponseEntity<EventSalesResponse> getEventSales(@PathVariable UUID eventId) {
        EventSalesResponse response = service.getEventSales(eventId);
        return ResponseEntity.ok(response);
    }
}