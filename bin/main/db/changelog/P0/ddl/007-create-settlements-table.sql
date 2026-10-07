-- liquibase formatted sql

-- changeset liquibase:007-create-settlements-table
CREATE TABLE settlements (
    settlement_id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(group_id),
    debtor BIGINT NOT NULL REFERENCES users(user_id),
    creditor BIGINT NOT NULL REFERENCES users(user_id),
    amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
