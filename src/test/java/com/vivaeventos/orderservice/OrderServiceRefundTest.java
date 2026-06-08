package com.vivaeventos.orderservice;

import com.vivaeventos.orderservice.dto.RefundRequest;
import com.vivaeventos.orderservice.dto.OrderResponse;
import com.vivaeventos.orderservice.exception.OrderNotFoundException;
import com.vivaeventos.orderservice.kafka.OrderEventPublisher;
import com.vivaeventos.orderservice.module.Order;
import com.vivaeventos.orderservice.repository.OrderRepository;
import com.vivaeventos.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.vivaeventos.orderservice.service.AuditLogService;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceRefundTest {

    @Mock private OrderRepository repository;
    @Mock private OrderEventPublisher publisher;
    @Mock private AuditLogService auditLogService;
    @InjectMocks private OrderService orderService;

    private UUID orderId;
    private UUID eventId;
    private UUID customerId;
    private Order confirmedOrder;

    @BeforeEach
    void setUp() {
        orderId    = UUID.randomUUID();
        eventId    = UUID.randomUUID();
        customerId = UUID.randomUUID();

        confirmedOrder = Order.builder()
                .id(orderId)
                .eventId(eventId)
                .customerId(customerId)
                .ticketType("GENERAL")
                .quantity(2)
                .unitPrice(new BigDecimal("120000"))
                .discountPct(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("240000"))
                .status("CONFIRMED")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(repository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Criterio 1: "Dado que el evento fue cancelado cuando el cliente solicita
    // devolución entonces el sistema debe registrar la solicitud."
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void dadoOrdenConfirmada_cuandoSeSolicitaDevolucion_entoncesEstadoCambiaARefundRequested() {
        // GIVEN
        when(repository.findById(orderId)).thenReturn(Optional.of(confirmedOrder));
        RefundRequest request = new RefundRequest("cliente@email.com", "EVENTO_CANCELADO");

        // WHEN
        OrderResponse response = orderService.requestRefund(orderId, request);

        // THEN — criterio 1: solicitud registrada en BD
        assertThat(response.status()).isEqualTo("REFUND_REQUESTED");
        verify(repository).save(any(Order.class));
    }

    @Test
    void dadoOrdenConfirmada_cuandoSeSolicitaDevolucion_entoncesSePublicaEventoEnKafka() {
        // GIVEN
        when(repository.findById(orderId)).thenReturn(Optional.of(confirmedOrder));
        RefundRequest request = new RefundRequest("cliente@email.com", "EVENTO_CANCELADO");

        // WHEN
        orderService.requestRefund(orderId, request);

        // THEN — se publica en Kafka para que notification-service envíe confirmación
        verify(publisher).publishRefundRequested(any(Order.class), eq("cliente@email.com"), eq("EVENTO_CANCELADO"));
    }

    @Test
    void dadoOrdenConfirmada_cuandoSeSolicitaDevolucion_entoncesEventoKafkaContieneMontoCorrect() {
        // GIVEN
        when(repository.findById(orderId)).thenReturn(Optional.of(confirmedOrder));
        RefundRequest request = new RefundRequest("cliente@email.com", "EVENTO_CANCELADO");

        // WHEN
        orderService.requestRefund(orderId, request);

        // THEN — el evento de Kafka tiene el monto correcto para mostrarlo en el email
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(publisher).publishRefundRequested(
                orderCaptor.capture(),
                eq("cliente@email.com"),
                eq("EVENTO_CANCELADO")
        );
        assertThat(orderCaptor.getValue().getTotalAmount())
                .isEqualByComparingTo(new BigDecimal("240000"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Casos de error
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void dadoOrdenNoExiste_cuandoSeSolicitaDevolucion_entoncesLanzaOrderNotFoundException() {
        // GIVEN
        when(repository.findById(orderId)).thenReturn(Optional.empty());
        RefundRequest request = new RefundRequest("cliente@email.com", "EVENTO_CANCELADO");

        // WHEN / THEN
        assertThatThrownBy(() -> orderService.requestRefund(orderId, request))
                .isInstanceOf(OrderNotFoundException.class);

        // No debe publicar nada en Kafka
        verify(publisher, never()).publishRefundRequested(any(), any(), any());
    }

    @Test
    void dadoOrdenPendiente_cuandoSeSolicitaDevolucion_entoncesLanzaIllegalStateException() {
        // GIVEN — orden en estado PENDING (no pagada aún)
        confirmedOrder.setStatus("PENDING");
        when(repository.findById(orderId)).thenReturn(Optional.of(confirmedOrder));
        RefundRequest request = new RefundRequest("cliente@email.com", "EVENTO_CANCELADO");

        // WHEN / THEN — no se puede devolver algo que no está pagado
        assertThatThrownBy(() -> orderService.requestRefund(orderId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");

        verify(publisher, never()).publishRefundRequested(any(), any(), any());
    }

    @Test
    void dadoOrdenCancelada_cuandoSeSolicitaDevolucion_entoncesLanzaIllegalStateException() {
        // GIVEN — orden ya cancelada
        confirmedOrder.setStatus("CANCELLED");
        when(repository.findById(orderId)).thenReturn(Optional.of(confirmedOrder));
        RefundRequest request = new RefundRequest("cliente@email.com", "EVENTO_CANCELADO");

        // WHEN / THEN
        assertThatThrownBy(() -> orderService.requestRefund(orderId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CANCELLED");

        verify(publisher, never()).publishRefundRequested(any(), any(), any());
    }

    @Test
    void dadoOrdenYaEnDevolucion_cuandoSeSolicitaDevolucionDeNuevo_entoncesLanzaIllegalStateException() {
        // GIVEN — orden que ya tiene una solicitud de devolución en curso
        confirmedOrder.setStatus("REFUND_REQUESTED");
        when(repository.findById(orderId)).thenReturn(Optional.of(confirmedOrder));
        RefundRequest request = new RefundRequest("cliente@email.com", "EVENTO_CANCELADO");

        // WHEN / THEN — no se puede solicitar devolución duplicada
        assertThatThrownBy(() -> orderService.requestRefund(orderId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("REFUND_REQUESTED");

        verify(publisher, never()).publishRefundRequested(any(), any(), any());
    }
}