ALTER TABLE vocabulary_topic ADD COLUMN public_id TEXT;
ALTER TABLE vocabulary ADD COLUMN public_audio_id TEXT;
ALTER TABLE vocabulary ADD COLUMN public_image_id TEXT;
ALTER TABLE vocabulary ADD COLUMN example_meaning TEXT;
ALTER TABLE vocabulary_test_question ADD COLUMN public_id TEXT;
ALTER TABLE grammar_topic ADD COLUMN public_id TEXT;
ALTER TABLE listening_topic ADD COLUMN public_id TEXT;
ALTER TABLE listening ADD COLUMN public_audio_id TEXT;
ALTER TABLE listening ADD COLUMN public_image_id TEXT;
ALTER TABLE listening_test_question ADD COLUMN public_audio_id TEXT;
ALTER TABLE listening_test_question ADD COLUMN public_image_id TEXT;

ALTER TABLE vocabulary_test_question ADD COLUMN image_url TEXT;
ALTER TABLE vocabulary_test_question ADD COLUMN explanation TEXT;
ALTER TABLE grammar_test_question ADD COLUMN explanation TEXT;
