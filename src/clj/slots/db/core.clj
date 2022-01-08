(ns slots.db.core
  (:require
    [cheshire.core :refer [generate-string parse-string]]
    [next.jdbc.date-time]
    [next.jdbc.prepare]
    [clojure.pprint :refer [pprint]]
    [next.jdbc.result-set]
    [clojure.tools.logging :as log]
    [conman.core :as conman]
    [slots.config :refer [env]]
    [mount.core :refer [defstate]]
    [slots.db.helpers :refer [ts]]
    [java-time :as jt]
    [slots.routes.data :as data])
  (:import (org.postgresql.util PGobject)))
;(java.sql Timestamp)
;(java.time LocalDateTime format.DateTimeFormatter)))


(defstate ^:dynamic *db*
          :start (if-let [jdbc-url (env :database-url)]
                   (conman/connect! {:jdbc-url jdbc-url})
                   (do
                     (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
                     *db*))
          :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn pgobj->clj [^org.postgresql.util.PGobject pgobj]
  (let [type (.getType pgobj)
        value (.getValue pgobj)]
    (case type
      "json" (parse-string value true)
      "jsonb" (parse-string value true)
      "citext" (str value)
      value)))

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v))
  java.sql.Array
  (read-column-by-label [^java.sql.Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^java.sql.Array v _2 _3]
    (vec (.getArray v)))
  org.postgresql.util.PGobject
  (read-column-by-label [^org.postgresql.util.PGobject pgobj _]
    (pgobj->clj pgobj))
  (read-column-by-index [^org.postgresql.util.PGobject pgobj _2 _3]
    (pgobj->clj pgobj)))

(defn clj->jsonb-pgobj [value]
  (doto (PGobject.)
    (.setType "jsonb")
    (.setValue (generate-string value))))

(extend-protocol next.jdbc.prepare/SettableParameter
  clojure.lang.IPersistentMap
  (set-parameter [^clojure.lang.IPersistentMap v ^java.sql.PreparedStatement stmt ^long idx]
    (.setObject stmt idx (clj->jsonb-pgobj v)))
  clojure.lang.IPersistentVector
  (set-parameter [^clojure.lang.IPersistentVector v ^java.sql.PreparedStatement stmt ^long idx]
    (let [conn (.getConnection stmt)
          meta (.getParameterMetaData stmt)
          type-name (.getParameterTypeName meta idx)]
      (if-let [elem-type (when (= (first type-name) \_)
                           (apply str (rest type-name)))]
        (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
        (.setObject stmt idx (clj->jsonb-pgobj v))))))

(defn availabilities-for-period [start end preferred-tutors]
  (group-by
    (juxt #(jt/local-date (:start_at %)) (juxt :tutor_id :name))
    (sort-by
      :start_at
      (get-availabilities-for-period
        {:period-start     start
         :period-end       end
         :preferred-tutors preferred-tutors}))))

;(availabilities-for-period (ts 6 9 0) (ts 7 11 0) [])

(defn sessions-for-period [start end preferred-tutors]
  (group-by
    (juxt #(jt/local-date (:start_at %)) (juxt :tutor_id :name))
    (get-speaking-sessions-for-period
      {:period-start     start
       :period-end       end
       :preferred-tutors preferred-tutors})))

;(sessions-for-period (ts 6 9 0) (ts 7 9 0) nil)

(defn to-5min-slot [dt]
  (/ (/ (jt/as dt :second-of-day) 60) 5))

(defn leading-zero [t]
  (if (< t 10)
    (str "0" t)
    (str t)))

(defn slot-to-time
  "Converts 5 min slot of the day into its time representation. E.g. 96 -> 08:00."
  [s]
  (let [day-mins (* 5 s)
        time-hour (quot day-mins 60)
        time-mins (rem day-mins 60)]
    (str (leading-zero time-hour) ":" (leading-zero time-mins))))

(defn split-increasing
  "Splits collection into chunks of sequences which elements increase by 1."
  [c]
  (reduce
    (fn [acc n]
      (let [prev (peek (peek acc))]
        (if (and prev (= -1 (- prev n)))
          (update acc (dec (count acc)) conj n)
          (conj acc [n]))))
    []
    c))

(defn arrange-free-slots-to-duration
  "Returns only those periods of 5min chunks which are longer or equal the duration."
  [slots duration]
  (filter
    (fn [col]
      (let [start (* 5 (first col))
            end (* 5 (last col))]
        (>= (- end start) duration))) (split-increasing slots)))

(defn available-slots
  [period-start period-end duration preferred-tutors]
  (let [availabilities (availabilities-for-period period-start period-end preferred-tutors)
        reserved-sessions (sessions-for-period period-start period-end preferred-tutors)
        day-5min-grid (range 96 241)]                       ; from 8 AM to 8 PM
    {:duration         duration
     :preferred-tutors (if preferred-tutors (str preferred-tutors) "all")
     :calendar         (for [[[date tutor] tutor-avs-for-date] availabilities
                             :let [tutor-reserved-for-date (get reserved-sessions [date tutor])
                                   available-5min-slots (apply sorted-set
                                                               (mapcat
                                                                 #(range
                                                                    (to-5min-slot (:start_at %))
                                                                    (inc (to-5min-slot (:end_at %))))
                                                                 tutor-avs-for-date))
                                   reserved-5min-slots (apply sorted-set
                                                              (mapcat
                                                                #(range
                                                                   (to-5min-slot (:start_at %))
                                                                   (inc
                                                                     (to-5min-slot
                                                                       (jt/plus (:start_at %)
                                                                                (jt/minutes (:duration %))
                                                                                (jt/minutes (:break %))))))
                                                                tutor-reserved-for-date))
                                   free-periods (arrange-free-slots-to-duration
                                                  (clojure.set/difference
                                                    available-5min-slots
                                                    reserved-5min-slots)
                                                  duration)
                                   free-5min-slots (apply sorted-set (apply concat free-periods))]]

                         {:date                    date
                          :tutor-id                (get tutor 0)
                          :tutor-name              (get tutor 1)
                          :free-5min-slots         free-5min-slots
                          :free-periods-timestamps (mapv
                                                     (fn [p]
                                                       (let [start (* 5 (first p))
                                                             end (* 5 (last p))]
                                                         {:start (jt/plus (jt/local-date-time date 0 0) (jt/minutes start))
                                                          :end   (jt/plus (jt/local-date-time date 0 0) (jt/minutes end))}))
                                                     free-periods)
                          :slots-by-type           (map (fn [slot-num]
                                                          {:num  slot-num
                                                           :time (slot-to-time slot-num)
                                                           :type (cond
                                                                   (boolean (some #(= % slot-num) free-5min-slots)) :free
                                                                   (boolean (some #(= % slot-num) reserved-5min-slots)) :reserved
                                                                   (boolean (some #(= % slot-num) available-5min-slots)) :available
                                                                   :else :empty)})
                                                        day-5min-grid)})}))

;; Seed DB
(defn insert-tutors! [] (doseq [t data/tutors] (create-tutor! t)))
(defn insert-availabilities! [] (doseq [a data/availabilities] (create-availability! a)))
(defn insert-speaking-sessions! [] (doseq [s data/sessions] (create-speaking-session! s)))

(comment
  (do
    (insert-tutors!)
    (insert-availabilities!)
    (insert-speaking-sessions!))

  #_end_of_comment)
