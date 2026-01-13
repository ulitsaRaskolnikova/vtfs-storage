--liquibase formatted sql

--changeset vtfs:005-initialize-root-directory
--comment: Initialize root directory with ino=1000 and set sequence to start from 1001

-- Вставляем корневую директорию с явным указанием ino
INSERT INTO inodes (ino, mode, size, nlink) 
VALUES (1000, 16877, 0, 2)
ON CONFLICT (ino) DO NOTHING;

-- Настраиваем sequence так, чтобы следующий ino был 1001
SELECT setval('inodes_ino_seq', 1000, true);
