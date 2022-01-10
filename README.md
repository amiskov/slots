# Reserve a session with a Tutor
There's a platform where students learn a foreign language by doing exercises and participating in speaking sessions with tutors.

A student can invite a tutor for a speaking session.

## Data Model
There are tutors with a `break` property which represents their resting period after each speaking session:

```
tutors:
- id PK
- name TEXT
- break INT (minutes)
```

Tutor's availability is represented with records of this form:

```
availabilities:
- id PK
- start_at TIMESTAMP
- end_at TIMESTAMP
- tutor_id REFERENCES tutors(id)
```

Tutors choose availability periods by hand for each day.

Speaking session for a tutor:

```
speaking_sessions:
- id PK
- start_at TIMESTAMP
- duration INT (minutes)
- tutor_id REFERENCES tutors(id)
```

A speaking session can be reserved inside a 5-minute time grid (e.g. 16:00, 16:05, 16:55, etc.).

### The Task
There are:

- an array of tutors;
- an array of tutors availabilities;
- an array of tutors reserved speaking sessions.

A student wants to reserve a speaking session. He sets the `period-start` and `period-end` parameters and the desired
duration of the session. He also may (or may not) choose several preferred tutors (array of their IDs). We need to get
an array of time slots where the given tutors are available for reserving a speaking session with given duration.

Example: find all available time slots for all tutors for a 20-minute speaking session from 2022-01-06 to 2022-01-08.