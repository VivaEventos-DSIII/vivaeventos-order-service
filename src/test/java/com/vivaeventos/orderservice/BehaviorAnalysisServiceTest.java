package com.vivaeventos.orderservice;

import com.vivaeventos.orderservice.dto.CustomerBehaviorResponse;
import com.vivaeventos.orderservice.repository.OrderRepository;
import com.vivaeventos.orderservice.service.BehaviorAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BehaviorAnalysisServiceTest {

    @Mock private OrderRepository orderRepository;
    @InjectMocks private BehaviorAnalysisService behaviorAnalysisService;

    private UUID customerId;
    private UUID eventId1;
    private UUID eventId2;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        eventId1   = UUID.randomUUID();
        eventId2   = UUID.randomUUID();
    }

    // helper para crear listas de Object[] sin problemas de tipos
    private List<Object[]> listOf(Object[]... arrays) {
        List<Object[]> list = new ArrayList<>();
        for (Object[] arr : arrays) list.add(arr);
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Criterio 1: identificar patrones
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void dadoClienteConCompras_cuandoSeAnaliza_entoncesIdentificaPatronDeTipoBoletaPreferida() {
        when(orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED")).thenReturn(3L);
        when(orderRepository.sumTotalSpentByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new BigDecimal("750000"));
        when(orderRepository.findTicketTypeBreakdownByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(
                        new Object[]{"VIP",     2L, 2, new BigDecimal("500000")},
                        new Object[]{"GENERAL", 1L, 1, new BigDecimal("120000")}
                ));
        when(orderRepository.findTopEventsByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(new Object[]{eventId1, 2L, 2, new BigDecimal("500000")}));

        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);

        assertThat(response.preferredTicketType()).isEqualTo("VIP");
        assertThat(response.totalOrders()).isEqualTo(3);
        assertThat(response.totalSpent()).isEqualByComparingTo(new BigDecimal("750000"));
    }

    @Test
    void dadoClienteConCompras_cuandoSeAnaliza_entoncesCalculaPromedioCorrectamente() {
        when(orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED")).thenReturn(4L);
        when(orderRepository.sumTotalSpentByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new BigDecimal("480000"));
        when(orderRepository.findTicketTypeBreakdownByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(new Object[]{"GENERAL", 4L, 4, new BigDecimal("480000")}));
        when(orderRepository.findTopEventsByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new ArrayList<>());

        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);

        assertThat(response.averageOrderValue()).isEqualByComparingTo(new BigDecimal("120000.00"));
    }

    @Test
    void dadoClienteConCompras_cuandoSeAnaliza_entoncesRetornaDesglosePorTipoBoleta() {
        when(orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED")).thenReturn(2L);
        when(orderRepository.sumTotalSpentByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new BigDecimal("240000"));
        when(orderRepository.findTicketTypeBreakdownByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(new Object[]{"GENERAL", 2L, 2, new BigDecimal("240000")}));
        when(orderRepository.findTopEventsByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(new Object[]{eventId1, 2L, 2, new BigDecimal("240000")}));

        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);

        assertThat(response.ticketTypeBreakdown()).hasSize(1);
        assertThat(response.ticketTypeBreakdown().get(0).ticketType()).isEqualTo("GENERAL");
    }

    @Test
    void dadoClienteConCompras_cuandoSeAnaliza_entoncesRetornaTopEventos() {
        when(orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED")).thenReturn(3L);
        when(orderRepository.sumTotalSpentByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new BigDecimal("360000"));
        when(orderRepository.findTicketTypeBreakdownByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(new Object[]{"GENERAL", 3L, 3, new BigDecimal("360000")}));
        when(orderRepository.findTopEventsByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(
                        new Object[]{eventId1, 2L, 2, new BigDecimal("240000")},
                        new Object[]{eventId2, 1L, 1, new BigDecimal("120000")}
                ));

        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);

        assertThat(response.topEvents()).hasSize(2);
        assertThat(response.topEvents().get(0).eventId()).isEqualTo(eventId1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Criterio 2: generar recomendaciones
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void dadoClienteVIP_cuandoSeGeneranRecomendaciones_entoncesRecomendaEventosPremium() {
        when(orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED")).thenReturn(2L);
        when(orderRepository.sumTotalSpentByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new BigDecimal("500000"));
        when(orderRepository.findTicketTypeBreakdownByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(new Object[]{"VIP", 2L, 2, new BigDecimal("500000")}));
        when(orderRepository.findTopEventsByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new ArrayList<>());

        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);

        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.recommendations().get(0)).containsIgnoringCase("premium");
    }

    @Test
    void dadoClienteFrecuente_cuandoSeGeneranRecomendaciones_entoncesRecomendaFidelidad() {
        when(orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED")).thenReturn(5L);
        when(orderRepository.sumTotalSpentByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new BigDecimal("600000"));
        when(orderRepository.findTicketTypeBreakdownByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(new Object[]{"GENERAL", 5L, 5, new BigDecimal("600000")}));
        when(orderRepository.findTopEventsByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new ArrayList<>());

        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);

        String todasRecomendaciones = String.join(" ", response.recommendations());
        assertThat(todasRecomendaciones).containsIgnoringCase("fidelidad");
    }

    @Test
    void dadoClienteSinHistorial_cuandoSeAnaliza_entoncesDevuelveRespuestaVaciaConRecomendacion() {
        when(orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED")).thenReturn(0L);

        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);

        assertThat(response.totalOrders()).isEqualTo(0);
        assertThat(response.recommendations()).isNotEmpty();
        assertThat(response.preferredTicketType()).isEqualTo("N/A");
    }

    @Test
    void dadoClienteConCompras_cuandoSeAnaliza_entoncesResumenNoEsNulo() {
        when(orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED")).thenReturn(1L);
        when(orderRepository.sumTotalSpentByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new BigDecimal("120000"));
        when(orderRepository.findTicketTypeBreakdownByCustomer(customerId, "CONFIRMED"))
                .thenReturn(listOf(new Object[]{"GENERAL", 1L, 1, new BigDecimal("120000")}));
        when(orderRepository.findTopEventsByCustomer(customerId, "CONFIRMED"))
                .thenReturn(new ArrayList<>());

        CustomerBehaviorResponse response = behaviorAnalysisService.analyzeBehavior(customerId);

        assertThat(response.summary()).isNotBlank();
        assertThat(response.summary()).contains("GENERAL");
    }
}