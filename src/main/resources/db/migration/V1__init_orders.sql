-- V1__init_orders.sql
-- Schema inicial del order-service
-- Historias: US-04, US-07, US-13, RQ-06 (0 sobreventa), RQ-14 (idempotencia)

CREATE TABLE promo_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50)    NOT NULL UNIQUE,
    discount_pct    NUMERIC(5, 2)  NOT NULL CHECK (discount_pct BETWEEN 0 AND 100),
    max_uses        INTEGER,
    used_count      INTEGER        NOT NULL DEFAULT 0,
    valid_from      TIMESTAMP      NOT NULL,
    valid_until     TIMESTAMP      NOT NULL,
    event_id        UUID,          -- NULL = aplica a todos los eventos
    active          BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        UUID           NOT NULL,
    customer_id     UUID           NOT NULL,
    ticket_type     VARCHAR(50)    NOT NULL DEFAULT 'GENERAL',  -- GENERAL | VIP | STUDENT
    quantity        INTEGER        NOT NULL DEFAULT 1 CHECK (quantity > 0),
    unit_price      NUMERIC(12, 2) NOT NULL,
    discount_pct    NUMERIC(5, 2)  NOT NULL DEFAULT 0,
    total_amount    NUMERIC(12, 2) NOT NULL,
    promo_code_id   UUID           REFERENCES promo_codes(id),
    status          VARCHAR(50)    NOT NULL DEFAULT 'PENDING',
    -- PENDING | AWAITING_PAYMENT | PAYMENT_PROCESSING | CONFIRMED | CANCELLED | REFUNDED
    idempotency_key VARCHAR(255)   UNIQUE,   -- RQ-14: evitar cobros duplicados
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_event_id    ON orders(event_id);
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status      ON orders(status);
