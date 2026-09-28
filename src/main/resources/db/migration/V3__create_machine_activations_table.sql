CREATE TABLE machine_activations(

id UUID PRIMARY KEY,
license_id UUID NOT NULL REFERENCES licenses(id) ON DELETE RESTRICT,
machine_fingerprint_hash VARCHAR(64) NOT NULL,
machine_name VARCHAR(150),
activated_at TIMESTAMP WITH TIME ZONE default CURRENT_TIMESTAMP NOT NULL,
last_seen_at TIMESTAMP WITH TIME ZONE default CURRENT_TIMESTAMP NOT NULL,
deactivated_at TIMESTAMP WITH TIME ZONE

);

CREATE INDEX idx_machine_activations_license_id
    ON machine_activations(license_id);


CREATE UNIQUE INDEX uq_machine_activations_active_machine
    ON machine_activations(license_id, machine_fingerprint_hash)
    WHERE deactivated_at IS NULL;