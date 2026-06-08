package com.vivaeventos.orderservice.kafka;

import com.vivaeventos.orderservice.module.Order;
import com.vivaeventos.orderservice.kafka.event.OrderCancelledEvent;
import com.vivaeventos.orderservice.kafka.event.OrderConfirmedEvent;
import com.vivaeventos.orderservice.kafka.event.OrderCreatedEvent;
import com.vivaeventos.orderservice.kafka.event.RefundRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topics.order-confirmed}")
    private String orderConfirmedTopic;

    @Value("${kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    // Nuevo topic para devoluciones
    @Value("${kafka.topics.order-refund-requested}")
    private String orderRefundRequestedTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(), order.getEventId(), order.getCustomerId(),
                order.getTicketType(), order.getQuantity(), order.getUnitPrice(),
                order.getDiscountPct(), order.getTotalAmount(), order.getCreatedAt()
        );
        kafkaTemplate.send(orderCreatedTopic, order.getId().toString(), event);
    }

    public void publishOrderConfirmed(Order order) {
        String email = order.getCustomerEmail() != null ? order.getCustomerEmail() : "";
        String userName = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                order.getId(), order.getCustomerId(), email, userName,
                order.getTotalAmount(), order.getEventId(),
                order.getTicketType(), order.getQuantity(), order.getUnitPrice(),
                order.getDiscountPct(), order.getUpdatedAt()
        );
        kafkaTemplate.send(orderConfirmedTopic, order.getId().toString(), event);
    }

    public void publishOrderCancelled(Order order, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                order.getId(), order.getEventId(), order.getCustomerId(),
                order.getQuantity(), reason, order.getUpdatedAt()
        );
        kafkaTemplate.send(orderCancelledTopic, order.getId().toString(), event);
    }

    /**
     * Publica el evento de solicitud de devolución en Kafka.
     * El notification-service lo consume para enviar el email de confirmación.
     *
     * @param order     la orden con estado REFUND_REQUESTED
     * @param userEmail email del cliente para que notification-service pueda enviarle el correo
     * @param reason    motivo de la devolución
     */
    public void publishRefundRequested(Order order, String userEmail, String reason) {
        RefundRequestedEvent event = new RefundRequestedEvent(
                order.getId(),
                order.getEventId(),
                order.getCustomerId(),
                userEmail,
                order.getTotalAmount(),
                reason,
                order.getUpdatedAt()
        );
        kafkaTemplate.send(orderRefundRequestedTopic, order.getId().toString(), event);
    }
}