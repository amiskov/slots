create table availabilities (
id SERIAL PRIMARY KEY,
start_at TIMESTAMP,
end_at TIMESTAMP,
tutor_id INTEGER REFERENCES tutors(id) ON DELETE CASCADE
);