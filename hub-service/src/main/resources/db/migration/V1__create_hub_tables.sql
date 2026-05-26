CREATE TABLE p_hubs
(
    id             UUID           NOT NULL,
    name           VARCHAR(100)   NOT NULL,
    hub_type       VARCHAR(20)    NOT NULL,
    zip_code       VARCHAR(10),
    address        VARCHAR(255)   NOT NULL,
    detail_address VARCHAR(255),
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    manager_id     UUID,
    capacity       INTEGER,
    deleted_reason VARCHAR(500),
    created_at     TIMESTAMP      NOT NULL,
    created_by     UUID,
    updated_at     TIMESTAMP      NOT NULL,
    updated_by     UUID,
    deleted_at     TIMESTAMP,
    deleted_by     UUID,
    CONSTRAINT pk_hubs PRIMARY KEY (id),
    CONSTRAINT uq_hubs_name UNIQUE (name)
);

CREATE TABLE p_hub_routes
(
    id                     UUID          NOT NULL,
    origin_hub_id          UUID          NOT NULL,
    destination_hub_id     UUID          NOT NULL,
    estimated_duration_min INTEGER       NOT NULL,
    estimated_distance_km  DECIMAL(8, 2) NOT NULL,
    created_at             TIMESTAMP     NOT NULL,
    created_by             UUID,
    updated_at             TIMESTAMP     NOT NULL,
    updated_by             UUID,
    deleted_at             TIMESTAMP,
    deleted_by             UUID,
    CONSTRAINT pk_hub_routes PRIMARY KEY (id),
    CONSTRAINT uq_hub_routes UNIQUE (origin_hub_id, destination_hub_id)
);

CREATE TABLE p_stock_transfers
(
    id                  UUID         NOT NULL,
    from_hub_id         UUID         NOT NULL,
    to_hub_id           UUID         NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    total_product_count INTEGER      NOT NULL,
    manager_id          UUID,
    dispatched_at       TIMESTAMP,
    completed_at        TIMESTAMP,
    note                VARCHAR(500),
    created_at          TIMESTAMP    NOT NULL,
    created_by          UUID,
    updated_at          TIMESTAMP    NOT NULL,
    updated_by          UUID,
    deleted_at          TIMESTAMP,
    deleted_by          UUID,
    CONSTRAINT pk_stock_transfers PRIMARY KEY (id)
);
