-- Tables behind the asynchronous side of the system.

-- Transactional outbox: events are inserted in the same transaction as the claim change and
-- published to Kafka by a relay. The identity column gives the publishing order.
CREATE TABLE outbox_event (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id     UUID         NOT NULL UNIQUE,
    aggregate_id UUID         NOT NULL,
    event_type   VARCHAR(100) NOT NULL,
    payload      JSONB        NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    published_at TIMESTAMPTZ
);

-- The relay only ever looks for unpublished rows, so index just those.
CREATE INDEX idx_outbox_event_unpublished ON outbox_event (id) WHERE published_at IS NULL;

-- Event identifiers the exposure projection has already applied (idempotent consumer).
CREATE TABLE processed_event (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);

-- Read model: open claims and outstanding amount per market, line of business and currency.
CREATE TABLE exposure_summary (
    country_code       CHAR(2)        NOT NULL,
    line_of_business   VARCHAR(20)    NOT NULL,
    currency           CHAR(3)        NOT NULL,
    open_claim_count   BIGINT         NOT NULL,
    outstanding_amount NUMERIC(16, 2) NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (country_code, line_of_business, currency)
);
