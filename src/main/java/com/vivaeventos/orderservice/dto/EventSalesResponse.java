package com.vivaeventos.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de respuesta para el reporte de ventas de un evento.
 *
 * ¿Por qué un DTO separado y no reusar OrderResponse?
 * - OrderResponse representa UNA orden individual.
 * - EventSalesResponse representa un AGREGADO de todas las órdenes de un evento.
 * - Son conceptos distintos → DTOs distintos (principio de responsabilidad única).
 *
 * Criterios de aceptación cubiertos:
 * - Criterio 1: totalRevenue → "mostrar el total vendido"
 * - Criterio 2: ticketsSold  → "mostrar número de boletas vendidas"
 */
public class EventSalesResponse {

    /**
     * UUID del evento consultado.
     * Permite al organizador confirmar que está viendo el reporte correcto.
     */
    private UUID eventId;

    /**
     * Número total de boletas vendidas (suma de quantity de órdenes CONFIRMED).
     * Criterio US-10: "el sistema debe mostrar número de boletas vendidas"
     */
    private Integer ticketsSold;

    /**
     * Ingresos totales generados (suma de totalAmount de órdenes CONFIRMED).
     * Criterio US-10: "el sistema debe mostrar el total vendido"
     */
    private BigDecimal totalRevenue;

    /**
     * Número total de órdenes confirmadas.
     * Una orden puede tener quantity > 1 (compra de múltiples boletas).
     */
    private Integer totalOrders;

    /**
     * Mensaje descriptivo del estado del reporte.
     * Ejemplos:
     * - "Se han vendido 150 boletas con ingresos de $18.000.000"
     * - "No hay ventas registradas para este evento"
     */
    private String message;

    /**
     * Constructor vacío requerido por Jackson.
     */
    public EventSalesResponse() {}

    /**
     * Constructor completo para crear el reporte con todos los datos.
     *
     * @param eventId      UUID del evento
     * @param ticketsSold  total de boletas vendidas
     * @param totalRevenue ingresos totales
     * @param totalOrders  número de órdenes confirmadas
     * @param message      mensaje descriptivo del reporte
     */
    public EventSalesResponse(UUID eventId,
                              Integer ticketsSold,
                              BigDecimal totalRevenue,
                              Integer totalOrders,
                              String message) {
        this.eventId = eventId;
        this.ticketsSold = ticketsSold;
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.message = message;
    }

    // ── Getters y Setters ──────────────────────────────────────────────

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public Integer getTicketsSold() { return ticketsSold; }
    public void setTicketsSold(Integer ticketsSold) { this.ticketsSold = ticketsSold; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public Integer getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}