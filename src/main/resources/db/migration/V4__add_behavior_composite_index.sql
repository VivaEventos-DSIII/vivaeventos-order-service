-- V2__add_behavior_composite_index.sql
-- Índice compuesto para las consultas de análisis de comportamiento (US-15)
-- Las queries de US-15 siempre filtran por customer_id AND status juntos;
-- un índice compuesto es sustancialmente más eficiente que dos índices separados.

CREATE INDEX idx_orders_customer_status ON orders(customer_id, status);
