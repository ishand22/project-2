CREATE TABLE customers (
  id BIGSERIAL PRIMARY KEY, name VARCHAR(160) NOT NULL, email VARCHAR(254) NOT NULL UNIQUE,
  company VARCHAR(160), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE assets (
  id BIGSERIAL PRIMARY KEY, asset_tag VARCHAR(40) NOT NULL UNIQUE, serial_number VARCHAR(100) NOT NULL UNIQUE,
  product VARCHAR(160) NOT NULL, model VARCHAR(120) NOT NULL,
  status VARCHAR(24) NOT NULL CHECK (status IN ('AVAILABLE','RESERVED','ALLOCATED','IN_SERVICE','RETIRED')),
  customer_id BIGINT REFERENCES customers(id), warranty_expiry DATE
);
CREATE INDEX assets_product_status_idx ON assets(product, status);
CREATE TABLE orders (
  id BIGSERIAL PRIMARY KEY, order_number VARCHAR(32) NOT NULL UNIQUE,
  idempotency_key VARCHAR(160) NOT NULL UNIQUE, customer_id BIGINT NOT NULL REFERENCES customers(id),
  status VARCHAR(24) NOT NULL, failure_reason VARCHAR(500), retry_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(), updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE order_items (
  id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  product VARCHAR(160) NOT NULL, quantity INTEGER NOT NULL CHECK (quantity > 0), license_type VARCHAR(48) NOT NULL
);
CREATE TABLE order_transitions (
  id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  from_state VARCHAR(24), to_state VARCHAR(24) NOT NULL, details VARCHAR(500), created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE TABLE entitlements (
  id BIGSERIAL PRIMARY KEY, entitlement_key VARCHAR(40) NOT NULL UNIQUE,
  customer_id BIGINT NOT NULL REFERENCES customers(id), order_id BIGINT NOT NULL REFERENCES orders(id),
  product VARCHAR(160) NOT NULL, license_type VARCHAR(48) NOT NULL, quantity INTEGER NOT NULL,
  status VARCHAR(20) NOT NULL, activated_at TIMESTAMPTZ, expires_at TIMESTAMPTZ
);
