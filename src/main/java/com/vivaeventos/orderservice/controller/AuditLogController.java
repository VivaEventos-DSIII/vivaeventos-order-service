package com.vivaeventos.orderservice.controller;

import com.vivaeventos.orderservice.dto.AuditLogResponse;
import com.vivaeventos.orderservice.service.AuditLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller para consultar el historial de auditoría de una orden.
 *
 * US-18: "Dado que ocurre un error cuando se revisa el historial
 *         entonces debe verse el flujo completo."
 */
@RestController
@RequestMapping("/api/orders")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * GET /api/orders/{id}/audit
     *
     * Retorna el historial completo de estados de una orden.
     *
     * Ejemplo de respuesta:
     * [
     *   {"previousStatus": null,       "newStatus": "PENDING",    "description": "Orden creada"},
     *   {"previousStatus": "PENDING",  "newStatus": "CONFIRMED",  "description": "Pago confirmado"},
     *   {"previousStatus": "CONFIRMED","newStatus": "REFUND_REQ", "description": "Devolución solicitada"}
     * ]
     */
    @GetMapping("/{id}/audit")
    public ResponseEntity<List<AuditLogResponse>> getAuditLog(@PathVariable UUID id) {
        return ResponseEntity.ok(auditLogService.getAuditLog(id));
    }
}