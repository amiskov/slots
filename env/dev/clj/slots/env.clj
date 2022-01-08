(ns slots.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [slots.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[slots started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[slots has shut down successfully]=-"))
   :middleware wrap-dev})
