package com.vivaeventos.orderservice;

import com.vivaeventos.orderservice.dto.AuditLogResponse;
import com.vivaeventos.orderservice.module.OrderAuditLog;
import com.vivaeventos.orderservice.repository.OrderAuditLogRepository;
import com.vivaeventos.orderservice.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests unitarios para US-18 — Registro del flujo de transacción.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogServiceTest {

    @Mock private OrderAuditLogRepository auditLogRepository;
    @InjectMocks private AuditLogService auditLogService;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        when(auditLogRepository.save(any(OrderAuditLog.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Criterio 1: "Dado que ocurre una compra cuando se registran eventos
    //              entonces el sistema debe guardar cada estado."
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void dadoCambioDeEstado_cuandoSeRegistra_entoncesSeGuardaEnBD() {
        // WHEN
        auditLogService.record(orderId, null, "PENDING",
                "Orden creada por el cliente", "CLIENT");

        // THEN — se guarda exactamente 1 registro
        verify(auditLogRepository, times(1)).save(any(OrderAuditLog.class));
    }

    @Test
    void dadoCambioDeEstado_cuandoSeRegistra_entoncesGuardaDatosCorrectos() {
        // WHEN
        auditLogService.record(orderId, "PENDING", "CONFIRMED",
                "Pago confirmado por la pasarela", "PAYMENT_GATEWAY");

        // THEN — verifica que los datos guardados son correctos
        ArgumentCaptor<OrderAuditLog> captor = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        OrderAuditLog saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getPreviousStatus()).isEqualTo("PENDING");
        assertThat(saved.getNewStatus()).isEqualTo("CONFIRMED");
        assertThat(saved.getDescription()).contains("pasarela");
        assertThat(saved.getTriggeredBy()).isEqualTo("PAYMENT_GATEWAY");
    }

    @Test
    void dadoCreacionDeOrden_cuandoSeRegistra_entoncesStatusAnteriorEsNull() {
        // GIVEN — al crear una orden no hay estado anterior
        // WHEN
        auditLogService.record(orderId, null, "PENDING",
                "Orden creada", "CLIENT");

        // THEN — previousStatus debe ser null
        ArgumentCaptor<OrderAuditLog> captor = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getPreviousStatus()).isNull();
        assertThat(captor.getValue().getNewStatus()).isEqualTo("PENDING");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Criterio 2: "Dado que ocurre un error cuando se revisa el historial
    //              entonces debe verse el flujo completo."
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void dadoHistorialDeOrden_cuandoSeConsulta_entoncesRetornaFlujoCompleto() {
        // GIVEN — simulamos 3 cambios de estado registrados
        List<OrderAuditLog> logs = List.of(
                buildLog(orderId, null,        "PENDING",    "Orden creada",       "CLIENT"),
                buildLog(orderId, "PENDING",   "CONFIRMED",  "Pago confirmado",    "PAYMENT_GATEWAY"),
                buildLog(orderId, "CONFIRMED", "REFUND_REQ", "Devolución solicitada", "CLIENT")
        );
        when(auditLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId)).thenReturn(logs);

        // WHEN
        List<AuditLogResponse> result = auditLogService.getAuditLog(orderId);

        // THEN — retorna los 3 eventos del flujo completo
        assertThat(result).hasSize(3);
        assertThat(result.get(0).newStatus()).isEqualTo("PENDING");
        assertThat(result.get(1).newStatus()).isEqualTo("CONFIRMED");
        assertThat(result.get(2).newStatus()).isEqualTo("REFUND_REQ");
    }

    @Test
    void dadoHistorialDeOrden_cuandoSeConsulta_entoncesOrdenCronologicoEsCorrecto() {
        // GIVEN
        List<OrderAuditLog> logs = List.of(
                buildLog(orderId, null,      "PENDING",   "Orden creada",    "CLIENT"),
                buildLog(orderId, "PENDING", "CONFIRMED", "Pago confirmado", "PAYMENT_GATEWAY")
        );
        when(auditLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId)).thenReturn(logs);

        // WHEN
        List<AuditLogResponse> result = auditLogService.getAuditLog(orderId);

        // THEN — el primer evento es la creación (null → PENDING)
        assertThat(result.get(0).previousStatus()).isNull();
        assertThat(result.get(0).newStatus()).isEqualTo("PENDING");
        // el segundo es la confirmación
        assertThat(result.get(1).previousStatus()).isEqualTo("PENDING");
        assertThat(result.get(1).newStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void dadoOrdenSinHistorial_cuandoSeConsulta_entoncesRetornaListaVacia() {
        // GIVEN
        when(auditLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of());

        // WHEN
        List<AuditLogResponse> result = auditLogService.getAuditLog(orderId);

        // THEN
        assertThat(result).isEmpty();
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private OrderAuditLog buildLog(UUID orderId, String prev, String next,
                                   String desc, String triggeredBy) {
        return OrderAuditLog.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .previousStatus(prev)
                .newStatus(next)
                .description(desc)
                .triggeredBy(triggeredBy)
                .createdAt(LocalDateTime.now())
                .build();
    }
}