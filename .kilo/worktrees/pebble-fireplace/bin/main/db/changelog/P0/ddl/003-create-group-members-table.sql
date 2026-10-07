-- liquibase formatted sql

-- changeset liquibase:003-create-group-members-table
CREATE TABLE group_members (
    group_id BIGINT NOT NULL REFERENCES groups(group_id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    PRIMARY KEY (group_id, user_id)
);
