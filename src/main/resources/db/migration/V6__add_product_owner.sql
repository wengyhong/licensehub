ALTER TABLE products
    ADD COLUMN owner_id UUID;

ALTER TABLE products
    ADD CONSTRAINT fk_products_owner
    FOREIGN KEY (owner_id)
    REFERENCES users(id)
    ON DELETE RESTRICT;

CREATE INDEX idx_products_owner_id
    ON products(owner_id);