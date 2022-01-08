(ns slots.routes.data
  (:require [slots.db.helpers :refer [ts]]
            [java-time :as jt]))

(def tutors
  [{:name "Shriram" :break 10}
   {:name "Svetlana" :break 20}
   {:name "Chang Li" :break 10}])

(def availabilities
  [; Shriram, Jan 6
   {:tutor-id 1 :start-at (ts 6 9 0) :end-at (ts 6 12 00)}
   {:tutor-id 1 :start-at (ts 6 15 0) :end-at (ts 6 17 00)}
   ; Shriram, Jan 7
   {:tutor-id 1 :start-at (ts 7 10 0) :end-at (ts 7 11 00)}
   {:tutor-id 1 :start-at (ts 7 17 0) :end-at (ts 7 18 00)}
   ; Shriram, Jan 8
   {:tutor-id 1 :start-at (ts 8 12 0) :end-at (ts 8 16 00)}
   ; Svetlana, Jan 6
   {:tutor-id 2 :start-at (ts 6 12 0) :end-at (ts 6 16 00)}
   ; Svetlana, Jan 7
   {:tutor-id 2 :start-at (ts 7 12 0) :end-at (ts 7 15 00)}
   ; Chang Li, Jan 6
   {:tutor-id 3 :start-at (ts 6 8 0) :end-at (ts 6 12 00)}
   {:tutor-id 3 :start-at (ts 6 17 0) :end-at (ts 6 18 00)}
   ; Chang Li, Jan 7
   {:tutor-id 3 :start-at (ts 7 8 0) :end-at (ts 7 13 00)}
   ; Chang Li, Jan 8
   {:tutor-id 3 :start-at (ts 8 8 0) :end-at (ts 8 11 00)}])

(def sessions
  [; Shriram, Jan 6
   {:tutor-id 1 :start-at (ts 6 9 0) :duration 15}
   ; Shriram, Jan 7
   {:tutor-id 1 :start-at (ts 7 10 15) :duration 20}
   ; Svetlana, Jan 6
   {:tutor-id 2 :start-at (ts 6 13 0) :duration 20}])

