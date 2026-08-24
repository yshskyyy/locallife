CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS business (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    category VARCHAR(255),
    address VARCHAR(255),
    rating DOUBLE PRECISION,
    created_at TIMESTAMP,
    longitude DOUBLE PRECISION,
    latitude DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS review (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT,
    rating DOUBLE PRECISION,
    comment VARCHAR(1000),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS voucher (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    stock INTEGER NOT NULL,
    begin_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS voucher_order (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    voucher_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_voucher_order_user_voucher UNIQUE (user_id, voucher_id)
);

CREATE INDEX IF NOT EXISTS idx_review_business_id ON review (business_id);
CREATE INDEX IF NOT EXISTS idx_voucher_order_voucher_id ON voucher_order (voucher_id);
