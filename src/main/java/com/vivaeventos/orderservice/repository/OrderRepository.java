package com.vivaeventos.orderservice.repository;

import com.vivaeventos.orderservice.module.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de órdenes de compra.
 * Extiende JpaRepository para operaciones CRUD básicas.
 * Agrega queries específicas para el reporte de ventas.
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Busca una orden por su clave de idempotencia.
     * Usado para evitar órdenes duplicadas (RQ-14).
     *
     * @param idempotencyKey clave única del cliente para esta operación
     * @return la orden si existe, vacío si no
     */
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    /**
     * Obtiene todas las órdenes CONFIRMADAS de un evento específico.
     * Solo las confirmadas representan ventas reales (US-10, criterio 1).
     *
     * @param eventId  UUID del evento
     * @param status   estado de la orden (se pasa "CONFIRMED")
     * @return lista de órdenes confirmadas del evento
     */
    List<Order> findByEventIdAndStatus(UUID eventId, String status);

    /**
     * Cuenta el total de boletas vendidas para un evento.
     * Suma la cantidad (quantity) de todas las órdenes CONFIRMADAS.
     *
     * Criterio: "mostrar número de boletas vendidas"
     *
     * @param eventId UUID del evento
     * @param status  estado de la orden (se pasa "CONFIRMED")
     * @return total de boletas vendidas, 0 si no hay ninguna
     */
    @Query("""
            SELECT COALESCE(SUM(o.quantity), 0)
            FROM Order o
            WHERE o.eventId = :eventId
              AND o.status = :status
            """)
    Integer countTicketsSoldByEventId(
            @Param("eventId") UUID eventId,
            @Param("status") String status
    );

    /**
     * Calcula el total de ingresos generados por un evento.
     * Suma el totalAmount de todas las órdenes CONFIRMADAS.
     *
     * Criterio:"mostrar el total vendido"
     *
     * @param eventId UUID del evento
     * @param status  estado de la orden (se pasa "CONFIRMED")
     * @return suma de ingresos, 0 si no hay ninguna
     */
    @Query("""
            SELECT COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.eventId = :eventId
              AND o.status = :status
            """)
    BigDecimal sumRevenueByEventId(
            @Param("eventId") UUID eventId,
            @Param("status") String status
    );
}