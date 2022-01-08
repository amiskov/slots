-- :name create-tutor! :! :n
-- :doc creates a new tutor
INSERT INTO tutors
(name, break)
VALUES (:name, :break)

-- :name create-availability! :! :n
-- :doc creates a new availability for the given tutor
INSERT INTO availabilities
(start_at, end_at, tutor_id)
VALUES (:start-at, :end-at, :tutor-id)

-- :name create-speaking-session! :! :n
-- :doc creates a new speaking session for the given tutor
INSERT INTO speaking_sessions
(start_at, duration, tutor_id)
VALUES (:start-at, :duration, :tutor-id)

-- :name get-tutors :? :*
-- :doc retrieves all tutors
select * from tutors;

-- :name get-availabilities-for-period :? :*
-- :doc retrieves all availabilities for the given period
select * from tutors
join
(select * from availabilities where start_at >= :period-start
 union select * from availabilities where end_at <= :period-end) as a
on tutors.id = a.tutor_id
--~(when (seq (:preferred-tutors params)) "where tutors.id in (:v*:preferred-tutors)")
;

-- :name get-speaking-sessions-for-period :? :*
-- :doc retrieves all availabilities for the given period
select * from tutors join
(select * from speaking_sessions where start_at >= :period-start
 and (start_at + duration * 60 * interval '1 second') <= :period-end) as s
on tutors.id = s.tutor_id
--~(when (seq (:preferred-tutors params)) "where tutors.id in (:v*:preferred-tutors)")
;
