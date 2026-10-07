-- liquibase formatted sql

-- changeset liquibase:004-create-expenses-table
CREATE TABLE expenses (
    expense_id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES groups(group_id),
    paid_by BIGINT NOT NULL REFERENCES users(user_id),
    amount NUMERIC(19, 2) NOT NULL,
    description TEXT,
    expense_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
