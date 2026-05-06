--changeset marcura:001-init
CREATE TABLE exchange_rate (
    id            BIGSERIAL      PRIMARY KEY,
    currency      VARCHAR(8)     NOT NULL,
    rate_date     DATE           NOT NULL,
    rate_to_base  NUMERIC(19, 8) NOT NULL,
    base_currency VARCHAR(8)     NOT NULL,
    fetched_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uk_exchange_rate_currency_date UNIQUE (currency, rate_date)
);

CREATE INDEX idx_exchange_rate_date     ON exchange_rate (rate_date DESC);
CREATE INDEX idx_exchange_rate_currency ON exchange_rate (currency);

CREATE TABLE currency_usage (
    currency        VARCHAR(8)  PRIMARY KEY,
    total_count     BIGINT      NOT NULL DEFAULT 0,
    last_queried_at TIMESTAMPTZ
);

CREATE TABLE shedlock (
    name        VARCHAR(64)  PRIMARY KEY,
    lock_until  TIMESTAMPTZ  NOT NULL,
    locked_at   TIMESTAMPTZ  NOT NULL,
    locked_by   VARCHAR(255) NOT NULL
);

--rollback DROP TABLE shedlock;
--rollback DROP TABLE currency_usage;
--rollback DROP INDEX idx_exchange_rate_currency;
--rollback DROP INDEX idx_exchange_rate_date;
--rollback DROP TABLE exchange_rate;
