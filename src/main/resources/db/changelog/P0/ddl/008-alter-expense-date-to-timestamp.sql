-- liquibase formatted sql

-- changeset liquibase:008-alter-expense-date-to-timestamp
-- expense_date is written as a UTC timestamp; a DATE column dropped the time and shifted
-- expenses added before 05:30 IST onto the previous day
ALTER TABLE expenses ALTER COLUMN expense_date TYPE TIMESTAMP USING expense_date::timestamp;
-- rollback ALTER TABLE expenses ALTER COLUMN expense_date TYPE DATE USING expense_date::date;
