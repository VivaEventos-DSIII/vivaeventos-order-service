-- V3__add_customer_email_to_orders.sql
-- Agrega el email del comprador al pedido para poder incluirlo en eventos Kafka
-- y que notification-service pueda enviar confirmaciones de compra.
ALTER TABLE orders ADD COLUMN customer_email VARCHAR(255);
