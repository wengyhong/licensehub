ALTER TABLE machine_activations
    ADD COLUMN activation_token_hash VARCHAR(64);

CREATE UNIQUE INDEX uq_machine_activations_activation_token_hash
    ON machine_activations (activation_token_hash)
    WHERE activation_token_hash IS NOT NULL;