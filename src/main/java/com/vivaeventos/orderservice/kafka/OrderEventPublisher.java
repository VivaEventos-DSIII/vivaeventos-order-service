package com.vivaeventos.orderservice.kafka;

import com.vivaeventos.orderservice.module.Order;
import com.vivaeventos.orderservice.kafka.event.OrderCancelledEvent;
import com.vivaeventos.orderservice.kafka.event.OrderConfirmedEvent;
import com.vivaeventos.orderservice.kafka.event.OrderCreatedEvent;
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

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getEventId(),
                order.getCustomerId(),
                order.getTicketType(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getDiscountPct(),
                order.getTotalAmount(),
                order.getCreatedAt()
        );
        kafkaTemplate.send(orderCreatedTopic, order.getId().toString(), event);
    }

    public void publishOrderConfirmed(Order order) {
        OrderConfirmedEvent event = new OrderConfirmedEvent(
                order.getId(),
                order.getEventId(),
                order.getCustomerId(),
                order.getTicketType(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.getDiscountPct(),
                order.getTotalAmount(),
                order.getUpdatedAt()
        );
        kafkaTemplate.send(orderConfirmedTopic, order.getId().toString(), event);
    }

    public void publishOrderCancelled(Order order, String reason) {
        OrderCancelledEvent event = new OrderCancelledEvent(
                order.getId(),
                order.getEventId(),
                order.getCustomerId(),
                order.getQuantity(),
                reason,
                order.getUpdatedAt()
        );
        kafkaTemplate.send(orderCancelledTopic, order.getId().toString(), event);
    }
}
