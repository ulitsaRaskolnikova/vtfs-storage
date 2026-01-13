--liquibase formatted sql

--changeset vtfs:004-create-indexes
--comment: Create indexes for performance

CREATE INDEX IF NOT EXISTS idx_directory_entries_parent ON directory_entries(parent_ino);
CREATE INDEX IF NOT EXISTS idx_directory_entries_inode ON directory_entries(inode_ino);
CREATE INDEX IF NOT EXISTS idx_file_data_inode_offset ON file_data(inode_ino, file_offset);
