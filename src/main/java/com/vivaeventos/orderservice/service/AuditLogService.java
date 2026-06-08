package com.vivaeventos.orderservice.service;

import com.vivaeventos.orderservice.dto.AuditLogResponse;
import com.vivaeventos.orderservice.exception.OrderNotFoundException;
import com.vivaeventos.orderservice.module.OrderAuditLog;
import com.vivaeventos.orderservice.repository.OrderAuditLogRepository;
import com.vivaeventos.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Servicio de auditoría del flujo de transacciones.
 *
 * US-18: "Como sistema quiero registrar el flujo completo de una
 *         transacción de compra para poder reconstruir incidentes."
 *
 * RESPONSABILIDADES:
 *  - Registrar cada cambio de estado de una orden (llamado desde OrderService)
 *  - Exponer el historial completo para reconstruir incidentes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final OrderAuditLogRepository auditLogRepository;
    private final OrderRepository orderRepository;

    /**
     * Registra un cambio de estado en el log de auditoría.
     *
     * Criterio 1: "Dado que ocurre una compra cuando se registran eventos
     *              entonces el sistema debe guardar cada estado."
     *
     * Se llama desde OrderService cada vez que cambia el estado de una orden:
     *  - Creación → PENDING
     *  - PENDING → PAYMENT_PROCESSING
     *  - PAYMENT_PROCESSING → CONFIRMED
     *  - CONFIRMED → REFUND_REQUESTED
     *  - Cualquier estado → CANCELLED
     *
     * @param orderId        ID de la orden
     * @param previousStatus estado anterior (null en la creación)
     * @param newStatus      nuevo estado
     * @param description    descripción legible del evento
     * @param triggeredBy    quién disparó el cambio (CLIENT, SYSTEM, PAYMENT_GATEWAY)
     */
    @Transactional
    public void record(UUID orderId, String previousStatus, String newStatus,
                       String description, String triggeredBy) {
        log.debug("Registrando auditoría: orden={} {}→{} por {}",
                orderId, previousStatus, newStatus, triggeredBy);

        OrderAuditLog entry = OrderAuditLog.builder()
                .orderId(orderId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .description(description)
                .triggeredBy(triggeredBy)
                .build();

        auditLogRepository.save(entry);
    }

    /**
     * Retorna el historial completo de una orden ordenado cronológicamente.
     *
     * Criterio 2: "Dado que ocurre un error cuando se revisa el historial
     *              entonces debe verse el flujo completo."
     *
     * Permite reconstruir exactamente qué pasó con una orden:
     * "este usuario creó la orden → se intentó pagar → la pasarela respondió X
     *  → luego se confirmó → luego el cliente solicitó devolución"
     *
     * @param orderId ID de la orden a consultar
     * @return lista de eventos ordenados de más antiguo a más reciente
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLog(UUID orderId) {
        log.info("Consultando historial de auditoría para orden {}", orderId);
        if (!orderRepository.existsById(orderId)) {
            throw new OrderNotFoundException(orderId);
        }
        return auditLogRepository
                .findByOrderIdOrderByCreatedAtAsc(orderId)
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }
}