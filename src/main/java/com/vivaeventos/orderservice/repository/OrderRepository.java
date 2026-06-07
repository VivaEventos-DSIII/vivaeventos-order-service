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
     * Solo las confirmadas representan ventas reales (criterio 1).
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
    /**
     * Obtiene ventas agrupadas por tipo de boleta para un evento.
     * Permite al organizador ver cuántas boletas GENERAL, VIP y STUDENT se vendieron.
     *
     * Criterio "el sistema debe mostrar métricas"
     *
     * Retorna una lista de arrays donde:
     * - [0] = ticketType (String)
     * - [1] = cantidad de órdenes (Long)
     * - [2] = total de boletas vendidas (Integer)
     * - [3] = ingresos por ese tipo (BigDecimal)
     *
     * @param eventId UUID del evento
     * @param status  estado de la orden (se pasa "CONFIRMED")
     */
    @Query("""
            SELECT o.ticketType,
                   COUNT(o),
                   COALESCE(SUM(o.quantity), 0),
                   COALESCE(SUM(o.totalAmount), 0)
            FROM Order o
            WHERE o.eventId = :eventId
              AND o.status = :status
            GROUP BY o.ticketType
            ORDER BY o.ticketType
            """)
    List<Object[]> findSalesByTicketType(
            @Param("eventId") UUID eventId,
            @Param("status") String status
    );

    /**
     * Obtiene ventas agrupadas por hora del día para un evento.
     * Permite identificar en qué horas se concentran las ventas (tendencias).
     *
     * Criterio: "dado que el organizador analiza datos entonces debe ver tendencias"
     *
     * Retorna una lista de arrays donde:
     * - [0] = hora del día 0-23 (Integer)
     * - [1] = cantidad de órdenes en esa hora (Long)
     * - [2] = total de boletas vendidas en esa hora (Integer)
     *
     * @param eventId UUID del evento
     * @param status  estado de la orden (se pasa "CONFIRMED")
     */
    @Query("""
            SELECT EXTRACT(hour from o.createdAt),
                   COUNT(o),
                   COALESCE(SUM(o.quantity), 0)
            FROM Order o
            WHERE o.eventId = :eventId
              AND o.status = :status
            GROUP BY EXTRACT(hour from o.createdAt)
            ORDER BY EXTRACT(hour from o.createdAt)
            """)
    List<Object[]> findSalesByHour(
            @Param("eventId") UUID eventId,
            @Param("status") String status
    );

    /**
     * Calcula el valor promedio de una orden para un evento.
     * Útil para entender el ticket promedio de compra.
     *
     * @param eventId UUID del evento
     * @param status  estado de la orden (se pasa "CONFIRMED")
     * @return promedio del totalAmount, null si no hay órdenes
     */
    @Query("""
            SELECT AVG(o.totalAmount)
            FROM Order o
            WHERE o.eventId = :eventId
              AND o.status = :status
            """)
    BigDecimal findAverageOrderValue(
            @Param("eventId") UUID eventId,
            @Param("status") String status
    );
}