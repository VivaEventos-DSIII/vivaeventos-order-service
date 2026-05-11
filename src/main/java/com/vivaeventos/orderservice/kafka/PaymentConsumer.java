package com.vivaeventos.orderservice.kafka;

import com.vivaeventos.orderservice.kafka.event.PaymentEvent;
import com.vivaeventos.orderservice.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentConsumer.class);

    private final OrderService orderService;

    public PaymentConsumer(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = "${kafka.topics.payment-confirmed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentConfirmed(PaymentEvent event) {
        log.info("Pago confirmado para orden {}", event.orderId());
        orderService.confirmOrder(event.orderId());
    }

    @KafkaListener(topics = "${kafka.topics.payment-failed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentFailed(PaymentEvent event) {
        log.warn("Pago fallido para orden {}", event.orderId());
        orderService.cancelOrder(event.orderId(), event.reason() != null ? event.reason() : "PAYMENT_FAILED");
    }

    @KafkaListener(topics = "${kafka.topics.payment-pending}", groupId = "${spring.kafka.consumer.group-id}")
    public void handlePaymentPending(PaymentEvent event) {
        log.info("Pago pendiente para orden {}", event.orderId());
        orderService.markPaymentPending(event.orderId());
    }
}
