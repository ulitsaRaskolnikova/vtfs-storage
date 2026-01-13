--liquibase formatted sql

--changeset vtfs:001-create-inodes-table
--comment: Create inodes table

CREATE TABLE IF NOT EXISTS inodes (
    ino BIGSERIAL PRIMARY KEY,
    mode INTEGER NOT NULL,
    size BIGINT NOT NULL DEFAULT 0,
    nlink INTEGER NOT NULL DEFAULT 1
);
