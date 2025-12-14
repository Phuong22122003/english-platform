ALTER TABLE user_answer
    ADD COLUMN audio_url TEXT,
    ADD COLUMN image_url TEXT,
    ADD COLUMN question TEXT,
    ADD COLUMN options JSONB,
    ADD COLUMN correct_answer VARCHAR(100),
    ADD COLUMN explanation TEXT;

ALTER TABLE exam_history
    ADD COLUMN name TEXT,
    ADD COLUMN duration INT;