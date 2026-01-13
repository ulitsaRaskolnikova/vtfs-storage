--liquibase formatted sql

--changeset vtfs:002-create-directory-entries-table
--comment: Create directory_entries table

CREATE TABLE IF NOT EXISTS directory_entries (
    id BIGSERIAL PRIMARY KEY,
    parent_ino BIGINT NOT NULL,
    name VARCHAR(256) NOT NULL,
    inode_ino BIGINT NOT NULL,
    CONSTRAINT uk_directory_entries_parent_name UNIQUE (parent_ino, name),
    CONSTRAINT fk_directory_entries_inode FOREIGN KEY (inode_ino) 
        REFERENCES inodes(ino) ON DELETE CASCADE
);
