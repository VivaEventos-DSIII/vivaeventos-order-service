package com.vivaeventos.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * DTO de respuesta para las estadísticas de ventas de un evento.
 *
 * Diferencia con EventSalesResponse:
 * - EventSalesResponse → reporte simple: total vendido y número de boletas.
 * - EventStatisticsResponse → análisis detallado: tendencias, desglose por tipo,
 *   ticket promedio, hora pico de ventas.
 *
 * @Data        → genera getters, setters, equals, hashCode y toString automáticamente.
 * @Builder     → permite construir el objeto con patrón builder (más legible que constructor).
 * @NoArgsConstructor → constructor vacío requerido por Jackson.
 * @AllArgsConstructor → constructor con todos los campos requerido por @Builder.
 *
 * Criterios de aceptación cubiertos:
 * - Criterio 1: métricas → ticketsSold, totalRevenue, averageOrderValue, salesByTicketType
 * - Criterio 2: tendencias → salesByHour, peakHour
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventStatisticsResponse {

    /** UUID del evento consultado */
    private UUID eventId;

    /**
     * Total de boletas vendidas (suma de quantity de órdenes CONFIRMED).
     * Criterio 1: métrica principal de volumen.
     */
    private Integer ticketsSold;

    /**
     * Ingresos totales generados (suma de totalAmount de órdenes CONFIRMED).
     * Criterio 1: métrica principal de ingresos.
     */
    private BigDecimal totalRevenue;

    /** Número total de órdenes confirmadas */
    private Integer totalOrders;

    /**
     * Valor promedio de una orden.
     * Criterio 1: métrica de ticket promedio.
     */
    private BigDecimal averageOrderValue;

    /**
     * Desglose de ventas por tipo de boleta (GENERAL, VIP, STUDENT).
     * Criterio 1: métricas detalladas por segmento.
     */
    private Map<String, TicketTypeStat> salesByTicketType;

    /**
     * Tendencia de ventas por hora del día (0-23).
     * Criterio 2: permite identificar cuándo se concentran las ventas.
     */
    private Map<Integer, HourlyStat> salesByHour;

    /**
     * Hora del día con mayor número de ventas (hora pico).
     * Criterio 2: tendencia más relevante para el organizador.
     */
    private Integer peakHour;

    /** Mensaje descriptivo del estado de las estadísticas */
    private String message;

    // ── Clase interna: estadísticas por tipo de boleta ────────────────

    /**
     * Métricas agrupadas por tipo de boleta.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketTypeStat {

        /** Número de órdenes de este tipo */
        private Long orders;

        /** Total de boletas vendidas de este tipo */
        private Integer tickets;

        /** Ingresos totales generados por este tipo */
        private BigDecimal revenue;
    }

    // ── Clase interna: estadísticas por hora ──────────────────────────

    /**
     * Métricas agrupadas por hora del día.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyStat {

        /** Número de órdenes en esta hora */
        private Long orders;

        /** Total de boletas vendidas en esta hora */
        private Integer tickets;
    }
}