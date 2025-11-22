CREATE TABLE toeic_test_group (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    release_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE toeic_test (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    total_completion INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    group_id VARCHAR(36),
    CONSTRAINT fk_test_group
        FOREIGN KEY (group_id) REFERENCES toeic_test_group(id)
);

CREATE TABLE toeic_test_question (
    id VARCHAR(36) PRIMARY KEY,
    test_id VARCHAR(36) NOT NULL,
    audio_url TEXT NOT NULL,
    image_url TEXT,
    public_audio_id TEXT,
    public_image_id TEXT,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer VARCHAR(100) NOT NULL,
    explanation TEXT,
    part INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_question_test
        FOREIGN KEY (test_id) REFERENCES toeic_test(id) ON DELETE CASCADE
);
