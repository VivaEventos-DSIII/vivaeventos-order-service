-- V2__init_order_audit_log.sql
-- US-18: Registro del flujo completo de una transacción de compra
-- "Dado que ocurre una compra cuando se registran eventos
--  entonces el sistema debe guardar cada estado."

CREATE TABLE order_audit_log (
                                 id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 order_id         UUID          NOT NULL REFERENCES orders(id),
                                 previous_status  VARCHAR(50),              -- NULL en la creación inicial
                                 new_status       VARCHAR(50)   NOT NULL,
                                 description      TEXT          NOT NULL,   -- descripción legible del evento
                                 triggered_by     VARCHAR(100),             -- quién disparó el cambio (sistema, cliente, pasarela)
                                 created_at       TIMESTAMP     NOT NULL DEFAULT now()
);

-- Índice para consultar el historial de una orden de forma eficiente
CREATE INDEX idx_audit_log_order_id ON order_audit_log(order_id);
CREATE INDEX idx_audit_log_created_at ON order_audit_log(created_at);