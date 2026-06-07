package com.vivaeventos.orderservice.service;

import com.vivaeventos.orderservice.dto.CustomerBehaviorResponse;
import com.vivaeventos.orderservice.dto.CustomerBehaviorResponse.EventPurchaseHistory;
import com.vivaeventos.orderservice.dto.CustomerBehaviorResponse.TicketTypePreference;
import com.vivaeventos.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de análisis de comportamiento de compra.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorAnalysisService {

    private final OrderRepository orderRepository;

    /**
     * Analiza el comportamiento de compra de un cliente e identifica patrones.
     *
     * Criterio 1: "Dado que existen datos de compra cuando el sistema analiza
     *              comportamiento entonces debe identificar patrones."
     *
     * @param customerId UUID del cliente a analizar
     * @return CustomerBehaviorResponse con patrones y recomendaciones
     */
    @Transactional(readOnly = true)
    public CustomerBehaviorResponse analyzeBehavior(UUID customerId) {
        log.info("Analizando comportamiento de compra para cliente {}", customerId);

        // 1. Contar total de órdenes
        long totalOrders = orderRepository.countOrdersByCustomerId(customerId, "CONFIRMED");

        // Si no hay compras, devolver respuesta vacía con recomendación genérica
        if (totalOrders == 0) {
            log.info("Cliente {} sin historial de compras", customerId);
            return new CustomerBehaviorResponse(
                    customerId, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                    "N/A", List.of(), List.of(),
                    List.of("Explora nuestros eventos disponibles para comenzar."),
                    "Este cliente no tiene compras registradas aún."
            );
        }

        // 2. Calcular total gastado
        BigDecimal totalSpent = orderRepository.sumTotalSpentByCustomer(customerId, "CONFIRMED");

        // 3. Calcular valor promedio por orden
        BigDecimal averageOrderValue = totalSpent.divide(
                BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);

        // 4. Desglose por tipo de boleta → identifica preferencia
        List<Object[]> ticketRaw = orderRepository
                .findTicketTypeBreakdownByCustomer(customerId, "CONFIRMED");

        List<TicketTypePreference> ticketBreakdown = new ArrayList<>();
        String preferredTicketType = "GENERAL"; // valor por defecto

        for (int i = 0; i < ticketRaw.size(); i++) {
            Object[] row = ticketRaw.get(i);
            String ticketType    = (String) row[0];
            long orders          = (Long) row[1];
            int ticketsBought    = ((Number) row[2]).intValue();
            BigDecimal spent     = (BigDecimal) row[3];

            ticketBreakdown.add(new TicketTypePreference(ticketType, orders, ticketsBought, spent));

            // El primer resultado es el más frecuente (ORDER BY COUNT DESC)
            if (i == 0) preferredTicketType = ticketType;
        }

        // 5. Top eventos comprados por el cliente
        List<Object[]> eventsRaw = orderRepository
                .findTopEventsByCustomer(customerId, "CONFIRMED");

        List<EventPurchaseHistory> topEvents = new ArrayList<>();
        for (Object[] row : eventsRaw) {
            UUID eventId      = (UUID) row[0];
            long orders       = (Long) row[1];
            int ticketsBought = ((Number) row[2]).intValue();
            BigDecimal spent  = (BigDecimal) row[3];
            topEvents.add(new EventPurchaseHistory(eventId, orders, ticketsBought, spent));
        }

        // 6. Generar recomendaciones basadas en los patrones identificados
        // Criterio 2: "dado que el sistema genera recomendaciones entonces
        //              estas deben mostrarse a los usuarios"
        List<String> recommendations = generateRecommendations(
                preferredTicketType, totalOrders, totalSpent, averageOrderValue);

        // 7. Construir mensaje resumen
        String summary = String.format(
                "Cliente con %d compra(s), ha gastado $%s en total. " +
                        "Prefiere boletas tipo %s con un ticket promedio de $%s.",
                totalOrders, totalSpent.toPlainString(),
                preferredTicketType, averageOrderValue.toPlainString()
        );

        log.info("Análisis completado para cliente {}: {} órdenes, preferencia {}",
                customerId, totalOrders, preferredTicketType);

        return new CustomerBehaviorResponse(
                customerId,
                (int) totalOrders,
                totalSpent,
                averageOrderValue,
                preferredTicketType,
                ticketBreakdown,
                topEvents,
                recommendations,
                summary
        );
    }

    /**
     * Genera recomendaciones basadas en los patrones identificados.
     *
     * Lógica de recomendación simple pero efectiva para el MVP:
     *  - Cliente VIP → recomendar eventos premium próximos
     *  - Cliente frecuente (3+ compras) → programa de fidelidad
     *  - Alto gasto → eventos exclusivos
     *  - Cliente nuevo (1 compra) → explorar categorías
     */
    private List<String> generateRecommendations(String preferredTicketType,
                                                 long totalOrders,
                                                 BigDecimal totalSpent,
                                                 BigDecimal averageOrderValue) {
        List<String> recommendations = new ArrayList<>();

        // Recomendación por tipo de boleta preferida
        switch (preferredTicketType) {
            case "VIP" -> recommendations.add(
                    "Como cliente VIP frecuente, te recomendamos explorar nuestros " +
                            "eventos premium y experiencias exclusivas.");
            case "STUDENT" -> recommendations.add(
                    "Hay nuevos eventos con descuento estudiantil disponibles " +
                            "que podrían interesarte.");
            default -> recommendations.add(
                    "Explora nuestra variedad de eventos en tu ciudad con " +
                            "boletas generales a los mejores precios.");
        }

        // Recomendación por frecuencia de compra
        if (totalOrders >= 5) {
            recommendations.add(
                    "Eres un cliente frecuente. Contáctanos para conocer " +
                            "nuestros beneficios de fidelidad y descuentos exclusivos.");
        } else if (totalOrders >= 3) {
            recommendations.add(
                    "¡Gracias por tu preferencia! Con tu próxima compra " +
                            "podrías acceder a beneficios especiales para clientes frecuentes.");
        } else {
            recommendations.add(
                    "Descubre más eventos disponibles en VivaEventos " +
                            "y encuentra tu próxima experiencia.");
        }

        // Recomendación por gasto total
        if (totalSpent.compareTo(new BigDecimal("500000")) >= 0) {
            recommendations.add(
                    "Tu historial de compras te hace elegible para " +
                            "acceso anticipado a la venta de nuevos eventos.");
        }

        return recommendations;
    }
}