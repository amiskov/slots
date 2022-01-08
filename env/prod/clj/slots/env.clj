(ns slots.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[slots started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[slots has shut down successfully]=-"))
   :middleware identity})
