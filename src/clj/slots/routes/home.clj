(ns slots.routes.home
  (:require
    [slots.layout :as layout]
    [slots.db.core :as db]
    [clojure.java.io :as io]
    [slots.middleware :as middleware]
    [ring.util.response]
    [java-time :as jt]
    [slots.db.helpers :refer [ts]]
    [ring.util.http-response :as response]))

(defn home-page [{:keys [params] :as request}]
  (let [{:keys [duration preferred-tutors]} params
        d (if duration (Integer/parseInt duration) 20)
        tutors (if preferred-tutors
                (if (vector? preferred-tutors)
                  (mapv #(Integer/parseInt %) preferred-tutors)
                  [(Integer/parseInt preferred-tutors)])
                nil)]
    (layout/render
      request
      "home.html"
      {:slots             (db/available-slots (ts 6 9 0) (ts 7 9 0) d tutors)
       :durations         (range 5 121 5)
       :selected-duration d
       :tutors            (mapv (fn [t]
                                  (if (some #(= % (:id t)) tutors)
                                    (assoc t :selected true)
                                    t))
                                (db/get-tutors))})))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/about" {:get about-page}]])

