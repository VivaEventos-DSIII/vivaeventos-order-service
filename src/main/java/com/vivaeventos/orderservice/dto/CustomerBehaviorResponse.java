package com.vivaeventos.orderservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para el análisis de comportamiento de compra del cliente.
 */
public record CustomerBehaviorResponse(

        // ── Identificación ────────────────────────────────────────────────
        UUID customerId,

        // ── Patrón 1: Resumen de compras ──────────────────────────────────
        // Criterio: "debe identificar patrones"
        int totalOrders,
        BigDecimal totalSpent,
        BigDecimal averageOrderValue,

        // ── Patrón 2: Tipo de boleta preferida ────────────────────────────
        // El tipo con más compras confirma la preferencia del cliente
        String preferredTicketType,

        // ── Patrón 3: Desglose por tipo de boleta ─────────────────────────
        List<TicketTypePreference> ticketTypeBreakdown,

        // ── Patrón 4: Eventos más comprados ───────────────────────────────
        List<EventPurchaseHistory> topEvents,

        // ── Recomendaciones ───────────────────────────────────────────────
        // Criterio: "dado que el sistema genera recomendaciones entonces
        //            estas deben mostrarse a los usuarios"
        List<String> recommendations,

        // Mensaje resumen del análisis
        String summary

) {
    /**
     * Desglose de preferencia por tipo de boleta.
     * Ejemplo: {ticketType: "VIP", orders: 3, totalSpent: 750000}
     */
    public record TicketTypePreference(
            String ticketType,
            long orders,
            int ticketsBought,
            BigDecimal totalSpent
    ) {}

    /**
     * Historial de compras por evento.
     * Ejemplo: {eventId: "...", orders: 2, ticketsBought: 4}
     */
    public record EventPurchaseHistory(
            UUID eventId,
            long orders,
            int ticketsBought,
            BigDecimal totalSpent
    ) {}
}