package com.vivaeventos.orderservice.repository;

import com.vivaeventos.orderservice.module.OrderAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio para el log de auditoría de órdenes.
 *
 * Spring Data JPA genera automáticamente el SQL para:
 *  - save(log) → INSERT INTO order_audit_log ...
 *  - findByOrderIdOrderByCreatedAtAsc → SELECT * FROM order_audit_log
 *      WHERE order_id = ? ORDER BY created_at ASC
 */
@Repository
public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, UUID> {

    /**
     * Retorna el historial completo de una orden ordenado cronológicamente.
     * Criterio 2: "dado que ocurre un error cuando se revisa el historial
     *              entonces debe verse el flujo completo."
     */
    List<OrderAuditLog> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}