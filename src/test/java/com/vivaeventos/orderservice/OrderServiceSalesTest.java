package com.vivaeventos.orderservice;

import com.vivaeventos.orderservice.module.Order;
import com.vivaeventos.orderservice.dto.EventSalesResponse;
import com.vivaeventos.orderservice.kafka.OrderEventPublisher;
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
 * Pruebas unitarias para getEventSales del OrderService (US-10).
 * Estrategia:
 * - Se mockean repository y publisher para no necesitar BD ni Kafka real.
 * - Cada test verifica un criterio de aceptación específico del US-10.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceSalesTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private OrderEventPublisher publisher;

    @InjectMocks
    private OrderService orderService;

    private UUID eventId;
    private Order ordenConfirmada1;
    private Order ordenConfirmada2;

    /**
     * Prepara dos órdenes confirmadas de prueba antes de cada test.
     */
    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();

        // Orden 1: 2 boletas GENERAL a $120.000 c/u = $240.000
        ordenConfirmada1 = Order.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .customerId(UUID.randomUUID())
                .ticketType("GENERAL")
                .quantity(2)
                .unitPrice(new BigDecimal("120000"))
                .discountPct(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("240000"))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Orden 2: 1 boleta VIP a $250.000
        ordenConfirmada2 = Order.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .customerId(UUID.randomUUID())
                .ticketType("VIP")
                .quantity(1)
                .unitPrice(new BigDecimal("250000"))
                .discountPct(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("250000"))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Criterio 1 (US-10): existen compras → muestra total vendido.
     * Verifica que totalRevenue e ingresos sean correctos.
     */
    @Test
    void dadoQueExistenVentas_cuandoSeConsultanVentas_entoncesDevuelveTotalVendido() {
        // GIVEN
        when(repository.findByEventIdAndStatus(eventId, "CONFIRMED"))
                .thenReturn(List.of(ordenConfirmada1, ordenConfirmada2));
        when(repository.countTicketsSoldByEventId(eventId, "CONFIRMED"))
                .thenReturn(3); // 2 + 1
        when(repository.sumRevenueByEventId(eventId, "CONFIRMED"))
                .thenReturn(new BigDecimal("490000")); // 240000 + 250000

        // WHEN
        EventSalesResponse response = orderService.getEventSales(eventId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getEventId()).isEqualTo(eventId);
        assertThat(response.getTotalRevenue())
                .isEqualByComparingTo(new BigDecimal("490000"));
        assertThat(response.getMessage()).contains("490000");
    }

    /**
     * Criterio 2 (US-10): muestra número de boletas vendidas.
     * Verifica que ticketsSold sea correcto.
     */
    @Test
    void dadoQueExistenVentas_cuandoSeConsultanVentas_entoncesDevuelveNumeroBoletas() {
        // GIVEN
        when(repository.findByEventIdAndStatus(eventId, "CONFIRMED"))
                .thenReturn(List.of(ordenConfirmada1, ordenConfirmada2));
        when(repository.countTicketsSoldByEventId(eventId, "CONFIRMED"))
                .thenReturn(3);
        when(repository.sumRevenueByEventId(eventId, "CONFIRMED"))
                .thenReturn(new BigDecimal("490000"));

        // WHEN
        EventSalesResponse response = orderService.getEventSales(eventId);

        // THEN
        assertThat(response.getTicketsSold()).isEqualTo(3);
        assertThat(response.getTotalOrders()).isEqualTo(2);
    }

    /**
     * Criterio: no hay ventas → devuelve reporte vacío con mensaje adecuado.
     */
    @Test
    void dadoQueNoExistenVentas_cuandoSeConsultanVentas_entoncesDevuelveMensajeVacio() {
        // GIVEN
        when(repository.findByEventIdAndStatus(eventId, "CONFIRMED"))
                .thenReturn(Collections.emptyList());

        // WHEN
        EventSalesResponse response = orderService.getEventSales(eventId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getTicketsSold()).isEqualTo(0);
        assertThat(response.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalOrders()).isEqualTo(0);
        assertThat(response.getMessage()).contains("No hay ventas");
    }

    /**
     * Criterio: una sola orden confirmada → reporte con datos correctos.
     */
    @Test
    void dadoQueExisteUnaOrden_cuandoSeConsultanVentas_entoncesDevuelveDatosCorrectos() {
        // GIVEN
        when(repository.findByEventIdAndStatus(eventId, "CONFIRMED"))
                .thenReturn(List.of(ordenConfirmada1));
        when(repository.countTicketsSoldByEventId(eventId, "CONFIRMED"))
                .thenReturn(2);
        when(repository.sumRevenueByEventId(eventId, "CONFIRMED"))
                .thenReturn(new BigDecimal("240000"));

        // WHEN
        EventSalesResponse response = orderService.getEventSales(eventId);

        // THEN
        assertThat(response.getTicketsSold()).isEqualTo(2);
        assertThat(response.getTotalOrders()).isEqualTo(1);
        assertThat(response.getTotalRevenue())
                .isEqualByComparingTo(new BigDecimal("240000"));
    }
}
