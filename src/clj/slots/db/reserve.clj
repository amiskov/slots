(ns slots.db.reserve
  (:require [java-time :as jt]
            [slots.db.helpers :refer [ts]]
            [slots.db.core :as db]))

(defn availabilities-for-period [start end preferred-tutors]
  (group-by
    (juxt #(jt/local-date (:start_at %)) (juxt :tutor_id :name))
    (sort-by
      :start_at
      (db/get-availabilities-for-period
        {:period-start     start
         :period-end       end
         :preferred-tutors preferred-tutors}))))

(defn sessions-for-period [start end preferred-tutors]
  (group-by
    (juxt #(jt/local-date (:start_at %)) (juxt :tutor_id :name))
    (db/get-speaking-sessions-for-period
      {:period-start     start
       :period-end       end
       :preferred-tutors preferred-tutors})))

(comment
  (availabilities-for-period (ts 6 9 0) (ts 7 11 0) [])
  (sessions-for-period (ts 6 9 0) (ts 7 9 0) nil)
  #_end_of_comment)

(defn ->5min-slot-num
  "Returns the number the 5-minute slot of the day of the given datetime object."
  [dt]
  (/ (/ (jt/as dt :second-of-day) 60) 5))

(defn slot->time
  "Converts 5 min slot of the day into its time representation. E.g. 96 -> 08:00."
  [s]
  (let [leading-zero #(if (< % 10) (str "0" %) (str %))
        minutes-of-the-day (* 5 s)
        time-hh (quot minutes-of-the-day 60)
        time-mm (rem minutes-of-the-day 60)]
    (str (leading-zero time-hh) ":" (leading-zero time-mm))))

(defn split-increasing
  "Splits collection into chunks of sequences increasing by 1. E.g. `[[1 2 3] [7 8 9]]`."
  [c]
  (reduce
    (fn [acc n]
      (let [prev (peek (peek acc))]
        (if (and prev (= -1 (- prev n)))
          (update acc (dec (count acc)) conj n)
          (conj acc [n]))))
    []
    c))

(defn fit-to-duration
  "Returns only those periods of 5-min slots which are longer or equal the duration."
  [slots duration]
  (filter #(>= (- (* 5 (last %)) (* 5 (first %)))
               duration)
          (split-increasing slots)))

(defn arrange-slots-by-date-and-tutors [availabilities reserved-sessions duration]
  (let [;; Day from 8 AM to 8 PM as a collection of 5 minutes slots.
        day-5min-grid (range 96 241)]
    (for [[[date tutor] tutor-availability-for-date] availabilities
          :let [tutor-sessions-for-date (get reserved-sessions [date tutor])
                ;; Initial availability of the tutor as a set of 5-min slots.
                tutor-initial-availability-slots (->> tutor-availability-for-date
                                                      (mapcat
                                                        #(range (->5min-slot-num (:start_at %))
                                                                (inc (->5min-slot-num (:end_at %)))))
                                                      (apply sorted-set))
                tutor-reserved-slots (->> tutor-sessions-for-date
                                          (mapcat
                                            #(let [session-end+break (jt/plus (:start_at %)
                                                                              (jt/minutes (:duration %))
                                                                              (jt/minutes (:break %)))]
                                               (range
                                                 (->5min-slot-num (:start_at %))
                                                 (inc (->5min-slot-num session-end+break)))))
                                          (apply sorted-set))
                ;; Collections of 5-min slots when the tutor is available for the given duration.
                tutor-availability-periods (fit-to-duration
                                             (clojure.set/difference tutor-initial-availability-slots
                                                                     tutor-reserved-slots)
                                             duration)
                tutor-available-slots (->> tutor-availability-periods
                                           (apply concat)
                                           (apply sorted-set))]]

      {:date                            date
       :tutor-id                        (get tutor 0)
       :tutor-name                      (get tutor 1)
       :current-availability-timestamps (let [slot->date-time #(jt/plus (jt/local-date-time date 0 0)
                                                                        (jt/minutes %))]
                                          (mapv (fn [p]
                                                  {:start (slot->date-time (* 5 (first p)))
                                                   :end   (slot->date-time (* 5 (last p)))})
                                                tutor-availability-periods))
       :slots-on-grid                   (let [slot-in? (fn [slots s] (boolean (some #(= % s) slots)))]
                                          (map (fn [slot]
                                                 {:num  slot
                                                  :time (slot->time slot)
                                                  :type (cond
                                                          (slot-in? tutor-available-slots slot) :currently-available
                                                          (slot-in? tutor-reserved-slots slot) :reserved
                                                          (slot-in? tutor-initial-availability-slots slot) :initially-available
                                                          :else :empty)})
                                               day-5min-grid))})))

(defn slots-for-period
  [period-start period-end duration preferred-tutors]
  (let [availabilities (availabilities-for-period period-start period-end preferred-tutors)
        reserved-sessions (sessions-for-period period-start period-end preferred-tutors)]
    {:duration         duration
     :preferred-tutors preferred-tutors
     :slots            (arrange-slots-by-date-and-tutors availabilities reserved-sessions duration)}))

