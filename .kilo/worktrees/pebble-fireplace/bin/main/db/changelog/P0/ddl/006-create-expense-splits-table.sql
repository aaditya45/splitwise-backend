-- liquibase formatted sql

-- changeset liquibase:006-create-expense-splits-table
CREATE TABLE expense_splits (
    split_id BIGSERIAL PRIMARY KEY,
    expense_id BIGINT NOT NULL REFERENCES expenses(expense_id),
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    amount NUMERIC(19, 2) NOT NULL,
    split_type VARCHAR(50) NOT NULL
);
