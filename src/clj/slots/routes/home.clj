(ns slots.routes.home
  (:require
    [slots.layout :as layout]
    [slots.db.core :as db]
    [clojure.java.io :as io]
    [slots.middleware :as middleware]
    [ring.util.response]
    [slots.db.helpers :refer [ts]]
    [slots.db.reserve :as reserve]
    [ring.util.http-response :as response]))

(defn home-page [{:keys [params] :as request}]
  (let [{:keys [duration preferred-tutors]} params
        d (if duration (Integer/parseInt duration) 15)
        tutors (if preferred-tutors
                 (if (vector? preferred-tutors)
                   (mapv #(Integer/parseInt %) preferred-tutors)
                   [(Integer/parseInt preferred-tutors)])
                 nil)]
    (layout/render
      request
      "home.html"
      {:slots-for-period  (reserve/slots-for-period (ts 6 9 0) (ts 7 9 0) d tutors)
       :durations         (range 15 121 5)                  ; available sessions in minutes
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

