-- Создание таблиц для Spring Security ACL
-- Эти таблицы используются для управления правами доступа

-- Таблица для хранения классов объектов
CREATE TABLE IF NOT EXISTS acl_class (
    id BIGSERIAL PRIMARY KEY,
    class VARCHAR(255) NOT NULL UNIQUE
);

-- Таблица для хранения SID (Security Identity) - пользователи и роли
CREATE TABLE IF NOT EXISTS acl_sid (
    id BIGSERIAL PRIMARY KEY,
    principal BOOLEAN NOT NULL,
    sid VARCHAR(255) NOT NULL,
    UNIQUE (principal, sid)
);

-- Таблица для хранения идентичности объектов
CREATE TABLE IF NOT EXISTS acl_object_identity (
    id BIGSERIAL PRIMARY KEY,
    object_id_class BIGINT NOT NULL,
    object_id_identity BIGINT NOT NULL,
    parent_object BIGINT,
    owner_sid BIGINT,
    entries_inheriting BOOLEAN NOT NULL,
    UNIQUE (object_id_class, object_id_identity),
    FOREIGN KEY (object_id_class) REFERENCES acl_class(id),
    FOREIGN KEY (parent_object) REFERENCES acl_object_identity(id),
    FOREIGN KEY (owner_sid) REFERENCES acl_sid(id)
);

-- Таблица для хранения записей ACL (права доступа)
CREATE TABLE IF NOT EXISTS acl_entry (
    id BIGSERIAL PRIMARY KEY,
    acl_object_identity BIGINT NOT NULL,
    ace_order INTEGER NOT NULL,
    sid BIGINT NOT NULL,
    mask INTEGER NOT NULL,
    granting BOOLEAN NOT NULL,
    audit_success BOOLEAN NOT NULL,
    audit_failure BOOLEAN NOT NULL,
    UNIQUE (acl_object_identity, ace_order),
    FOREIGN KEY (acl_object_identity) REFERENCES acl_object_identity(id) ON DELETE CASCADE,
    FOREIGN KEY (sid) REFERENCES acl_sid(id)
);

-- Создание индексов для улучшения производительности
CREATE INDEX IF NOT EXISTS idx_acl_object_identity_object_id_class ON acl_object_identity(object_id_class);
CREATE INDEX IF NOT EXISTS idx_acl_object_identity_parent_object ON acl_object_identity(parent_object);
CREATE INDEX IF NOT EXISTS idx_acl_object_identity_owner_sid ON acl_object_identity(owner_sid);
CREATE INDEX IF NOT EXISTS idx_acl_entry_acl_object_identity ON acl_entry(acl_object_identity);
CREATE INDEX IF NOT EXISTS idx_acl_entry_sid ON acl_entry(sid);
