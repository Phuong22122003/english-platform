INSERT INTO users (
    id,
    username,
    password,
    email,
    role,
    fullname
)
VALUES (
    gen_random_uuid()::varchar,
    'admin',
    '$2a$10$uB8lhJgWjS7OyS.FXe5MmexHGl0Wg9xvpoie0kU8Lr6pEg2LD368S',
    'hnguyenphuong09@gmail.com',
    'ADMIN',
    'System Administrator'
)
ON CONFLICT (username) DO NOTHING;
-- pass: admin123