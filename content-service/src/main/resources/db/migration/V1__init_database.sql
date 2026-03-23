-- postgresql database initialization

-- vocabulary, grammar, listening
CREATE TYPE item_type_enum AS ENUM ('vocabulary', 'grammar', 'listening', 'full_test');

CREATE TABLE vocabulary_topic(
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    image_url TEXT
);

CREATE TABLE vocabulary(
    id VARCHAR(36) PRIMARY KEY,
    topic_id VARCHAR(36) REFERENCES vocabulary_topic(id) ON DELETE CASCADE,
    word VARCHAR(100) NOT NULL,
    phonetic VARCHAR(100),
    meaning TEXT,
    example TEXT,
    audio_url TEXT,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vocabulary_test(
    id VARCHAR(36) PRIMARY KEY,
    topic_id VARCHAR(36) REFERENCES vocabulary_topic(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    duration INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vocabulary_test_question(
    id VARCHAR(36) PRIMARY KEY,
    test_id VARCHAR(36) REFERENCES vocabulary_test(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer VARCHAR(1) NOT NULL, -- A,B,C,D
    question_order INT
);

CREATE TABLE grammar_topic(
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE grammar(
    id VARCHAR(36) PRIMARY KEY,
    topic_id VARCHAR(36) REFERENCES grammar_topic(id) ON DELETE CASCADE,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE grammar_test(
    id VARCHAR(36) PRIMARY KEY,
    grammar_id VARCHAR(36) REFERENCES grammar(id) ON DELETE CASCADE,
    duration INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE grammar_test_question(
    id VARCHAR(36) PRIMARY KEY,
    test_id VARCHAR(36) REFERENCES grammar_test(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer VARCHAR(100) NOT NULL,
    question_order INT
);

CREATE TABLE listening_topic(
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE listening(
    id VARCHAR(36) PRIMARY KEY,
    topic_id VARCHAR(36) REFERENCES listening_topic(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    audio_url TEXT NOT NULL,
    image_url TEXT,
    transcript TEXT,
    question TEXT,
    options JSONB NOT NULL,
    correct_answer VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE listening_test(
    id VARCHAR(36) PRIMARY KEY,
    topic_id VARCHAR(36) REFERENCES listening_topic(id) ON DELETE CASCADE,
    name VARCHAR(100),
    duration INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE listening_test_question(
    id VARCHAR(36) PRIMARY KEY,
    test_id VARCHAR(36) REFERENCES listening_test(id) ON DELETE CASCADE,
    audio_url TEXT NOT NULL,
    image_url TEXT,
    question TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer VARCHAR(100) NOT NULL,
    explanation TEXT
);
