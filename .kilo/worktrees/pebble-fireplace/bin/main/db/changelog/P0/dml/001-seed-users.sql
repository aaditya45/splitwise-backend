-- liquibase formatted sql

-- changeset liquibase:001-seed-users
-- All users have password: password (BCrypt hash)
INSERT INTO users (email, name, password, phone, created_at, updated_at) VALUES
('alice@example.com', 'Alice', '$2a$10$slYQmyNdGzin7olVN3p5OPST9EwkPma3ou8HcUVDvnvQJqv8kvKFm', '1234567890', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('bob@example.com', 'Bob', '$2a$10$slYQmyNdGzin7olVN3p5OPST9EwkPma3ou8HcUVDvnvQJqv8kvKFm', '1234567891', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('charlie@example.com', 'Charlie', '$2a$10$slYQmyNdGzin7olVN3p5OPST9EwkPma3ou8HcUVDvnvQJqv8kvKFm', '1234567892', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('diana@example.com', 'Diana', '$2a$10$slYQmyNdGzin7olVN3p5OPST9EwkPma3ou8HcUVDvnvQJqv8kvKFm', '1234567893', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('evan@example.com', 'Evan', '$2a$10$slYQmyNdGzin7olVN3p5OPST9EwkPma3ou8HcUVDvnvQJqv8kvKFm', '1234567894', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

