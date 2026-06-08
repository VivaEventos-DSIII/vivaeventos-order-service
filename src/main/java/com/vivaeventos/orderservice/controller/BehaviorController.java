package com.vivaeventos.orderservice.controller;

import com.vivaeventos.orderservice.dto.CustomerBehaviorResponse;
import com.vivaeventos.orderservice.service.BehaviorAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller para el análisis de comportamiento de compra.
 */
@RestController
@RequestMapping("/api/orders/customers")
public class BehaviorController {

    private final BehaviorAnalysisService behaviorAnalysisService;

    public BehaviorController(BehaviorAnalysisService behaviorAnalysisService) {
        this.behaviorAnalysisService = behaviorAnalysisService;
    }

    /**
     * GET /api/orders/customers/{customerId}/behavior
     *
     * Analiza el comportamiento de compra de un cliente e identifica patrones.
     *
     * Criterio 1: "Dado que existen datos de compra cuando el sistema analiza
     *              comportamiento entonces debe identificar patrones."
     * Criterio 2: "Dado que el sistema genera recomendaciones entonces estas
     *              deben mostrarse a los usuarios."
     *
     * Ejemplo de respuesta:
     * {
     *   "customerId": "550e8400-...",
     *   "totalOrders": 5,
     *   "totalSpent": 600000,
     *   "preferredTicketType": "VIP",
     *   "recommendations": ["Como cliente VIP frecuente..."],
     *   "summary": "Cliente con 5 compras..."
     * }
     *
     * HTTP 200 → análisis generado (incluye caso sin historial ni compras registradas)
     */
    @GetMapping("/{customerId}/behavior")
    public ResponseEntity<CustomerBehaviorResponse> getCustomerBehavior(
            @PathVariable UUID customerId) {
        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);
        return ResponseEntity.ok(response);
    }
}