package com.vivaeventos.orderservice.service;

import com.vivaeventos.orderservice.dto.CreateOrderRequest;
import com.vivaeventos.orderservice.dto.EventSalesResponse;
import com.vivaeventos.orderservice.dto.EventStatisticsResponse;
import com.vivaeventos.orderservice.dto.EventStatisticsResponse.HourlyStat;
import com.vivaeventos.orderservice.dto.EventStatisticsResponse.TicketTypeStat;
import com.vivaeventos.orderservice.dto.OrderResponse;
import com.vivaeventos.orderservice.dto.RefundRequest;
import com.vivaeventos.orderservice.exception.OrderNotFoundException;
import com.vivaeventos.orderservice.kafka.OrderEventPublisher;
import com.vivaeventos.orderservice.module.Order;
import com.vivaeventos.orderservice.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final OrderEventPublisher publisher;

    public OrderService(OrderRepository repository, OrderEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        if (request.idempotencyKey() != null) {
            return repository.findByIdempotencyKey(request.idempotencyKey())
                    .map(OrderResponse::from)
                    .orElseGet(() -> OrderResponse.from(saveNewOrder(request)));
        }
        return OrderResponse.from(saveNewOrder(request));
    }

    private Order saveNewOrder(CreateOrderRequest request) {
        BigDecimal unitPrice = resolvePrecio(request.ticketType());
        BigDecimal discountPct = BigDecimal.ZERO;
        BigDecimal total = unitPrice
                .multiply(BigDecimal.valueOf(request.quantity()))
                .multiply(BigDecimal.ONE.subtract(
                        discountPct.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
                .setScale(2, RoundingMode.HALF_UP);

        Order order = Order.builder()
                .id(UUID.randomUUID())
                .eventId(request.eventId())
                .customerId(request.customerId())
                .ticketType(request.ticketType())
                .quantity(request.quantity())
                .unitPrice(unitPrice)
                .discountPct(discountPct)
                .totalAmount(total)
                .status("PENDING")
                .idempotencyKey(request.idempotencyKey())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order saved = repository.save(order);
        publisher.publishOrderCreated(saved);
        return saved;
    }

    public OrderResponse getOrderById(UUID orderId) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderResponse.from(order);
    }

    @Transactional
    public void confirmOrder(UUID orderId) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!"PAYMENT_PROCESSING".equals(order.getStatus()) && !"AWAITING_PAYMENT".equals(order.getStatus())) {
            log.warn("Ignorando confirmación de pago para orden {} en estado {}", orderId, order.getStatus());
            return;
        }
        order.setStatus("CONFIRMED");
        order.setUpdatedAt(LocalDateTime.now());
        repository.save(order);
        publisher.publishOrderConfirmed(order);
    }

    @Transactional
    public void cancelOrder(UUID orderId, String motivo) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if ("CANCELLED".equals(order.getStatus()) || "REFUND_REQUESTED".equals(order.getStatus())) {
            log.warn("Ignorando cancelación para orden {} en estado {}", orderId, order.getStatus());
            return;
        }
        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        repository.save(order);
        publisher.publishOrderCancelled(order, motivo);
    }

    @Transactional
    public void markPaymentPending(UUID orderId) {
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (!"PENDING".equals(order.getStatus()) && !"AWAITING_PAYMENT".equals(order.getStatus())) {
            log.warn("Ignorando transición a PAYMENT_PROCESSING para orden {} en estado {}", orderId, order.getStatus());
            return;
        }
        order.setStatus("PAYMENT_PROCESSING");
        order.setUpdatedAt(LocalDateTime.now());
        repository.save(order);
    }

    /**
     * Registra la solicitud de devolución.
     *
     * Criterio 1: "Dado que el evento fue cancelado cuando el cliente solicita
     * devolución entonces el sistema debe registrar la solicitud."
     *
     * Flujo:
     *  1. Buscar la orden por ID (lanza 404 si no existe)
     *  2. Validar que la orden puede ser devuelta (debe estar CONFIRMED)
     *  3. Cambiar estado a REFUND_REQUESTED
     *  4. Persistir en BD
     *  5. Publicar evento en Kafka → notification-service envía confirmación
     *
     * @param orderId ID de la orden a devolver
     * @param request DTO con email del cliente y motivo de devolución
     * @return OrderResponse con el estado actualizado
     */
    @Transactional
    public OrderResponse requestRefund(UUID orderId, RefundRequest request) {
        // 1. Buscar la orden
        Order order = repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 2. Validar que la orden puede ser devuelta
        // Solo se pueden devolver órdenes CONFIRMED (ya pagadas)
        if (!"CONFIRMED".equals(order.getStatus())) {
            throw new IllegalStateException(
                    "Solo se pueden devolver órdenes confirmadas. Estado actual: " + order.getStatus()
            );
        }

        // 3. Cambiar estado a REFUND_REQUESTED
        order.setStatus("REFUND_REQUESTED");
        order.setUpdatedAt(LocalDateTime.now());

        // 4. Persistir en BD
        Order saved = repository.save(order);

        // 5. Publicar evento en Kafka
        // notification-service lo consume y envía email de confirmación al cliente
        // (criterio 2: "dado que la devolución se procesa entonces el cliente
        //  debe recibir confirmación")
        publisher.publishRefundRequested(saved, request.userEmail(), request.reason());

        return OrderResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public EventSalesResponse getEventSales(UUID eventId) {
        List<Order> confirmedOrders = repository.findByEventIdAndStatus(eventId, "CONFIRMED");
        if (confirmedOrders.isEmpty()) {
            return new EventSalesResponse(eventId, 0, BigDecimal.ZERO, 0,
                    "No hay ventas registradas para este evento");
        }
        Integer ticketsSold = repository.countTicketsSoldByEventId(eventId, "CONFIRMED");
        BigDecimal totalRevenue = repository.sumRevenueByEventId(eventId, "CONFIRMED");
        Integer totalOrders = confirmedOrders.size();
        String message = String.format(
                "Se han vendido %d boletas en %d órdenes con ingresos totales de $%s",
                ticketsSold, totalOrders, totalRevenue.toPlainString());
        return new EventSalesResponse(eventId, ticketsSold, totalRevenue, totalOrders, message);
    }

    @Transactional(readOnly = true)
    public EventStatisticsResponse getEventStatistics(UUID eventId) {
        List<Order> confirmedOrders = repository.findByEventIdAndStatus(eventId, "CONFIRMED");
        if (confirmedOrders.isEmpty()) {
            return EventStatisticsResponse.builder()
                    .eventId(eventId).ticketsSold(0).totalRevenue(BigDecimal.ZERO)
                    .totalOrders(0).averageOrderValue(BigDecimal.ZERO)
                    .salesByTicketType(new HashMap<>()).salesByHour(new HashMap<>())
                    .peakHour(null).message("No hay ventas registradas para este evento")
                    .build();
        }
        Integer ticketsSold = repository.countTicketsSoldByEventId(eventId, "CONFIRMED");
        BigDecimal totalRevenue = repository.sumRevenueByEventId(eventId, "CONFIRMED");
        Integer totalOrders = confirmedOrders.size();
        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, TicketTypeStat> salesByTicketType = new HashMap<>();
        for (Object[] row : repository.findSalesByTicketType(eventId, "CONFIRMED")) {
            salesByTicketType.put((String) row[0],
                    TicketTypeStat.builder()
                            .orders((Long) row[1])
                            .tickets(((Number) row[2]).intValue())
                            .revenue((BigDecimal) row[3])
                            .build());
        }

        Map<Integer, HourlyStat> salesByHour = new HashMap<>();
        Integer peakHour = null;
        long maxOrders = 0;
        for (Object[] row : repository.findSalesByHour(eventId, "CONFIRMED")) {
            Integer hour = ((Number) row[0]).intValue();
            Long orderCount = (Long) row[1];
            salesByHour.put(hour, HourlyStat.builder()
                    .orders(orderCount)
                    .tickets(((Number) row[2]).intValue())
                    .build());
            if (orderCount > maxOrders) { maxOrders = orderCount; peakHour = hour; }
        }

        return EventStatisticsResponse.builder()
                .eventId(eventId).ticketsSold(ticketsSold).totalRevenue(totalRevenue)
                .totalOrders(totalOrders).averageOrderValue(averageOrderValue)
                .salesByTicketType(salesByTicketType).salesByHour(salesByHour)
                .peakHour(peakHour)
                .message(String.format("Estadísticas generadas: %d boletas vendidas, $%s en ingresos, hora pico: %s",
                        ticketsSold, totalRevenue.toPlainString(),
                        peakHour != null ? peakHour + ":00" : "N/A"))
                .build();
    }

    private BigDecimal resolvePrecio(String ticketType) {
        return switch (ticketType.toUpperCase()) {
            case "VIP"     -> new BigDecimal("250000");
            case "STUDENT" -> new BigDecimal("50000");
            default        -> new BigDecimal("120000");
        };
    }
}