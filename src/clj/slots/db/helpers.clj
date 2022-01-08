(ns slots.db.helpers
  (:require [java-time :as jt]))

;(defn now-ts []
;  (-> (LocalDateTime/now) Timestamp/valueOf))

(defn ts [d h m]
  (jt/sql-timestamp 2022 1 d h m))





