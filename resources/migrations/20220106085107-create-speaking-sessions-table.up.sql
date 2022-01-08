create table speaking_sessions (
id SERIAL PRIMARY KEY,
start_at TIMESTAMP,
duration INT,
tutor_id INTEGER REFERENCES tutors(id) ON DELETE CASCADE
);