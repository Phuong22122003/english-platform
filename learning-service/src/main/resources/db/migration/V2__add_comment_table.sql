CREATE TABLE comment (
    id VARCHAR(36) PRIMARY KEY,
    test_type item_type_enum NOT NULL,
    test_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    parent_id VARCHAR(36) NULL,
    reply_id VARCHAR(36) NULL,
    commented_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_parent_comment
        FOREIGN KEY (parent_id)
        REFERENCES comment(id)
        ON DELETE CASCADE
);