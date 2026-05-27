package com.vivaeventos.orderservice;

import com.vivaeventos.orderservice.dto.EventStatisticsResponse;
import com.vivaeventos.orderservice.kafka.OrderEventPublisher;
import com.vivaeventos.orderservice.module.Order;
import com.vivaeventos.orderservice.repository.OrderRepository;
import com.vivaeventos.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias para getEventStatistics del OrderService (US-14).
 *
 * Estrategia:
 * - Se mockean repository y publisher para no necesitar BD ni Kafka real.
 * - Cada test verifica un criterio de aceptación específico del US-14.
 * - Nombres en formato dado_cuando_entonces (BDD).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceStatisticsTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderEventPublisher publisher;

    @InjectMocks
    private OrderService orderService;

    private UUID eventId;
    private Order ordenGeneral;
    private Order ordenVip;

    /**
     * Prepara órdenes de prueba antes de cada test.
     * Una orden GENERAL con 2 boletas y una VIP con 1 boleta.
     */
    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();

        // Orden GENERAL: 2 boletas a $120.000 c/u = $240.000
        ordenGeneral = Order.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .customerId(UUID.randomUUID())
                .ticketType("GENERAL")
                .quantity(2)
                .unitPrice(new BigDecimal("120000"))
                .discountPct(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("240000"))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now().withHour(10))
                .updatedAt(LocalDateTime.now())
                .build();

        // Orden VIP: 1 boleta a $250.000
        ordenVip = Order.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .customerId(UUID.randomUUID())
                .ticketType("VIP")
                .quantity(1)
                .unitPrice(new BigDecimal("250000"))
                .discountPct(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("250000"))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now().withHour(11))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Criterio 1 (US-14): existen ventas → muestra métricas correctas.
     * Verifica ticketsSold, totalRevenue, totalOrders y averageOrderValue.
     */
    @Test
    void dadoQueExistenVentas_cuandoSeConsultanEstadisticas_entoncesDevuelveMetricas() {
        // GIVEN
        when(repository.findByEventIdAndStatus(eventId, "CONFIRMED"))
                .thenReturn(List.of(ordenGeneral, ordenVip));
        when(repository.countTicketsSoldByEventId(eventId, "CONFIRMED"))
                .thenReturn(3); // 2 + 1
        when(repository.sumRevenueByEventId(eventId, "CONFIRMED"))
                .thenReturn(new BigDecimal("490000"));
        when(repository.findSalesByTicketType(eventId, "CONFIRMED"))
                .thenReturn(List.of(
                        new Object[]{"GENERAL", 1L, 2, new BigDecimal("240000")},
                        new Object[]{"VIP", 1L, 1, new BigDecimal("250000")}
                ));
        when(repository.findSalesByHour(eventId, "CONFIRMED"))
                .thenReturn(List.of(
                        new Object[]{10, 1L, 2},
                        new Object[]{11, 1L, 1}
                ));

        // WHEN
        EventStatisticsResponse response = orderService.getEventStatistics(eventId);

        // THEN — criterio 1: métricas correctas
        assertThat(response).isNotNull();
        assertThat(response.getTicketsSold()).isEqualTo(3);
        assertThat(response.getTotalRevenue())
                .isEqualByComparingTo(new BigDecimal("490000"));
        assertThat(response.getTotalOrders()).isEqualTo(2);
        assertThat(response.getAverageOrderValue())
                .isEqualByComparingTo(new BigDecimal("245000.00"));
    }

    /**
     * Criterio 1 (US-14): desglose por tipo de boleta correcto.
     * Verifica que salesByTicketType contenga GENERAL y VIP con sus métricas.
     */
    @Test
    void dadoQueExistenVentas_cuandoSeConsultanEstadisticas_entoncesDevuelveDesgloseporTipo() {
        // GIVEN
        when(repository.findByEventIdAndStatus(eventId, "CONFIRMED"))
                .thenReturn(List.of(ordenGeneral, ordenVip));
        when(repository.countTicketsSoldByEventId(eventId, "CONFIRMED"))
                .thenReturn(3);
        when(repository.sumRevenueByEventId(eventId, "CONFIRMED"))
                .thenReturn(new BigDecimal("490000"));
        when(repository.findSalesByTicketType(eventId, "CONFIRMED"))
                .thenReturn(List.of(
                        new Object[]{"GENERAL", 1L, 2, new BigDecimal("240000")},
                        new Object[]{"VIP", 1L, 1, new BigDecimal("250000")}
                ));
        when(repository.findSalesByHour(eventId, "CONFIRMED"))
                .thenReturn(Collections.emptyList());

        // WHEN
        EventStatisticsResponse response = orderService.getEventStatistics(eventId);

        // THEN — desglose por tipo
        assertThat(response.getSalesByTicketType()).containsKey("GENERAL");
        assertThat(response.getSalesByTicketType()).containsKey("VIP");
        assertThat(response.getSalesByTicketType().get("GENERAL").getTickets()).isEqualTo(2);
        assertThat(response.getSalesByTicketType().get("VIP").getRevenue())
                .isEqualByComparingTo(new BigDecimal("250000"));
    }

    /**
     * Criterio 2 (US-14): tendencias por hora y hora pico correctas.
     * Verifica que salesByHour y peakHour sean correctos.
     */
    @Test
    void dadoQueExistenVentas_cuandoSeConsultanEstadisticas_entoncesDevuelveTendencias() {
        // GIVEN
        when(repository.findByEventIdAndStatus(eventId, "CONFIRMED"))
                .thenReturn(List.of(ordenGeneral, ordenVip));
        when(repository.countTicketsSoldByEventId(eventId, "CONFIRMED"))
                .thenReturn(3);
        when(repository.sumRevenueByEventId(eventId, "CONFIRMED"))
                .thenReturn(new BigDecimal("490000"));
        when(repository.findSalesByTicketType(eventId, "CONFIRMED"))
                .thenReturn(Collections.emptyList());
        when(repository.findSalesByHour(eventId, "CONFIRMED"))
                .thenReturn(List.of(
                        new Object[]{10, 1L, 2},  // hora 10: 1 orden, 2 boletas
                        new Object[]{11, 3L, 5}   // hora 11: 3 órdenes, 5 boletas (hora pico)
                ));

        // WHEN
        EventStatisticsResponse response = orderService.getEventStatistics(eventId);

        // THEN — criterio 2: tendencias
        assertThat(response.getSalesByHour()).containsKey(10);
        assertThat(response.getSalesByHour()).containsKey(11);
        assertThat(response.getPeakHour()).isEqualTo(11); // hora con más órdenes
        assertThat(response.getSalesByHour().get(11).getOrders()).isEqualTo(3L);
    }

    /**
     * Caso: no hay ventas → estadísticas vacías con mensaje adecuado.
     */
    @Test
    void dadoQueNoExistenVentas_cuandoSeConsultanEstadisticas_entoncesDevuelveVacio() {
        // GIVEN
        when(repository.findByEventIdAndStatus(eventId, "CONFIRMED"))
                .thenReturn(Collections.emptyList());

        // WHEN
        EventStatisticsResponse response = orderService.getEventStatistics(eventId);

        // THEN
        assertThat(response.getTicketsSold()).isEqualTo(0);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getPeakHour()).isNull();
        assertThat(response.getSalesByTicketType()).isEmpty();
        assertThat(response.getMessage()).contains("No hay ventas");
    }
}
