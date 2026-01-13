--liquibase formatted sql

--changeset vtfs:003-create-file-data-table
--comment: Create file_data table

CREATE TABLE IF NOT EXISTS file_data (
    id BIGSERIAL PRIMARY KEY,
    inode_ino BIGINT NOT NULL,
    file_offset BIGINT NOT NULL,
    data BYTEA NOT NULL,
    CONSTRAINT fk_file_data_inode FOREIGN KEY (inode_ino) 
        REFERENCES inodes(ino) ON DELETE CASCADE
);
