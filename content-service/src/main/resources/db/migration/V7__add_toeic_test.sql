--- 1. Bảng nhóm đề thi (Bộ đề)
CREATE TABLE toeic_test_group (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    release_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--- 2. Bảng đề thi chi tiết (Test 1, Test 2...)
CREATE TABLE toeic_test (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36),
    name VARCHAR(200) NOT NULL,
    total_completion INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_toeic_test__toeic_test_group__group_id
        FOREIGN KEY (group_id)
        REFERENCES toeic_test_group(id)
        ON DELETE CASCADE
);

--- 3. Bảng nhóm câu hỏi (Part 3, 4, 6, 7)
CREATE TABLE toeic_test_question_group (
    id VARCHAR(36) PRIMARY KEY,
    test_id VARCHAR(36) NOT NULL,
    passage_text TEXT,
    image_url TEXT,
    audio_url TEXT,
    public_audio_id TEXT,
    public_image_id TEXT,
    part INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_toeic_test_question_group__toeic_test__test_id
        FOREIGN KEY (test_id)
        REFERENCES toeic_test(id)
        ON DELETE CASCADE
);

--- 4. Bảng câu hỏi chi tiết
CREATE TABLE toeic_test_question (
    id VARCHAR(36) PRIMARY KEY,
    question_group_id VARCHAR(36) NOT NULL,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer VARCHAR(100) NOT NULL,
    explanation TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_toeic_test_question__toeic_test_question_group__group_id
        FOREIGN KEY (question_group_id)
        REFERENCES toeic_test_question_group(id)
        ON DELETE CASCADE
);