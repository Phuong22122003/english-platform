CREATE TYPE level AS ENUM ('BEGINNER', 'INTERMEDIATE', 'ADVANCED');

ALTER TABLE vocabulary_topic ADD COLUMN level level;
ALTER TABLE grammar_topic ADD COLUMN level level;
ALTER TABLE grammar ADD COLUMN level level;
ALTER TABLE listening_topic ADD COLUMN level level;
