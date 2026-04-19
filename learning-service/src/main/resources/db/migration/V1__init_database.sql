CREATE TYPE item_type_enum AS ENUM ('VOCABULARY', 'GRAMMAR', 'LISTENING', 'FULL_TEST');

CREATE TABLE exam_history (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    test_type item_type_enum NOT NULL,
    test_id VARCHAR(36) NOT NULL,
    name TEXT NOT NULL,
    score INT NOT NULL,
    duration INT NOT NULL,
    taken_at TIMESTAMP NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP NOT NULL
);

CREATE TABLE user_answer_group (
    id VARCHAR(36) PRIMARY KEY,
    exam_history_id VARCHAR(36) NOT NULL REFERENCES exam_history(id) ON DELETE CASCADE,
    passage_text TEXT,
    image_urls JSONB,
    audio_url TEXT,
    part INT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_answer (
    id VARCHAR(36) PRIMARY KEY,
    user_answer_group_id VARCHAR(36) NOT NULL  REFERENCES user_answer_group(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer VARCHAR(100) NOT NULL,
    explanation TEXT,
    selected_answer VARCHAR(10),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE favorite (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    item_type item_type_enum NOT NULL,
    item_id VARCHAR(36) NOT NULL,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plan (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    title VARCHAR(100) NOT NULL,
    description TEXT,
    is_completed BOOLEAN DEFAULT FALSE,
    start_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMP,
    target INT
);

CREATE TABLE plan_group (
    id VARCHAR(36) PRIMARY KEY,
    plan_id VARCHAR(36) REFERENCES plan(id) ON DELETE CASCADE,
    name VARCHAR(200),
    description TEXT,
    start_date TIMESTAMP,
    end_date TIMESTAMP
);

CREATE TABLE plan_detail (
    id VARCHAR(36) PRIMARY KEY,
    plan_group_id VARCHAR(36) REFERENCES plan_group(id) ON DELETE CASCADE,
    topic_type item_type_enum NOT NULL,
    topic_id VARCHAR(36),
    start_date TIMESTAMP,
    end_date TIMESTAMP
);