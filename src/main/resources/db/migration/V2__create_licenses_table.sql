CREATE TABLE licenses(

    id UUID PRIMARY KEY,
    product_id UUID REFERENCES products(id) ON DELETE RESTRICT,
    key_id VARCHAR(16) NOT NULL UNIQUE,
    key_hash VARCHAR(64) NOT NULL,
    customer_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_activations INT NOT NULL CHECK(max_activations > 0),
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_licenses_product_id
    ON licenses(product_id)