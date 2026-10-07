-- liquibase formatted sql

-- changeset liquibase:005-create-expense-participants-table
CREATE TABLE expense_participants (
    expense_id BIGINT NOT NULL REFERENCES expenses(expense_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    PRIMARY KEY (expense_id, user_id)
);
