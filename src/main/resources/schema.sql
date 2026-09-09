CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(32) NOT NULL UNIQUE,
    nickname VARCHAR(64) NOT NULL,
    role VARCHAR(16) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'MERCHANT')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS brand (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS business (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    category VARCHAR(255),
    address VARCHAR(255),
    rating DOUBLE PRECISION,
    created_at TIMESTAMP,
    longitude DOUBLE PRECISION,
    latitude DOUBLE PRECISION,
    brand_id BIGINT REFERENCES brand(id),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CLOSED')),
    merchant_id BIGINT
);

ALTER TABLE business ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE business ADD COLUMN IF NOT EXISTS merchant_id BIGINT;

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
    end_time TIMESTAMP NOT NULL,
    business_id BIGINT NOT NULL REFERENCES business(id)
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
CREATE INDEX IF NOT EXISTS idx_business_brand_id ON business (brand_id);
CREATE INDEX IF NOT EXISTS idx_voucher_business_id ON voucher (business_id);