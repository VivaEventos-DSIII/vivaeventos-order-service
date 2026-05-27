package com.vivaeventos.orderservice.controller;

import com.vivaeventos.orderservice.dto.CreateOrderRequest;
import com.vivaeventos.orderservice.dto.OrderResponse;
import com.vivaeventos.orderservice.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.vivaeventos.orderservice.dto.EventSalesResponse;
import com.vivaeventos.orderservice.dto.EventStatisticsResponse;

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
     * Consulta el reporte de ventas de un evento.
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
    /**
     * GET /api/orders/events/{eventId}/statistics
     *
     * Genera estadísticas detalladas de ventas de un evento.
     * Solo accesible para organizadores (rol validado en api-gateway).
     *
     * Diferencia con /sales:
     * - /sales    → reporte simple: total y número de boletas
     * - /statistics → análisis completo: tendencias, desglose por tipo, hora pico
     *
     * Respuestas posibles:
     * - HTTP 200 con datos  → hay ventas, devuelve métricas y tendencias
     * - HTTP 200 sin ventas → no hay ventas, mensaje informativo
     *
     * Ejemplo de llamada:
     * GET http://localhost:8082/api/orders/events/550e8400-e29b-41d4-a716-446655440000/statistics
     *
     * Ejemplo de respuesta:
     * {
     *   "eventId": "550e8400-...",
     *   "ticketsSold": 150,
     *   "totalRevenue": 18000000,
     *   "totalOrders": 95,
     *   "averageOrderValue": 189473.68,
     *   "salesByTicketType": {
     *     "GENERAL": { "orders": 80, "tickets": 120, "revenue": 14400000 },
     *     "VIP":     { "orders": 15, "tickets": 30,  "revenue": 3600000  }
     *   },
     *   "salesByHour": {
     *     "10": { "orders": 25, "tickets": 40 },
     *     "11": { "orders": 45, "tickets": 70 }
     *   },
     *   "peakHour": 11,
     *   "message": "Estadísticas generadas: 150 boletas vendidas..."
     * }
     */
    @GetMapping("/events/{eventId}/statistics")
    public ResponseEntity<EventStatisticsResponse> getEventStatistics(
            @PathVariable UUID eventId) {
        EventStatisticsResponse response = service.getEventStatistics(eventId);
        return ResponseEntity.ok(response);
    }
}