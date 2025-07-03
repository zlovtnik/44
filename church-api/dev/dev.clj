(ns dev
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.pprint :refer [pprint]]
            [church-api.core :as core]
            [church-api.schema :as schema]
            [church-api.db :as db]
            [io.pedestal.http :as http]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as lacinia-schema]))

;; System state
(defonce system nil)

(defn start
  "Starts the system for interactive development"
  []
  (alter-var-root #'system (fn [_] (core/start-server (core/create-server))))
  :started)

(defn stop
  "Stops the system"
  []
  (when system
    (alter-var-root #'system (fn [s] (http/stop s) nil)))
  :stopped)

(defn restart
  "Restarts the system"
  []
  (stop)
  (refresh :after 'dev/start))

;; Database helpers
(defn reset-db
  "Resets the database to a clean state"
  []
  (db/init-db!))

;; GraphQL helpers
(defn compile-schema
  "Compiles the GraphQL schema"
  []
  (-> (schema/load-schema)
      (lacinia-schema/compile)))

(defn execute-query
  "Execute a GraphQL query"
  [query-string variables]
  (let [compiled-schema (compile-schema)]
    (lacinia/execute compiled-schema query-string variables nil)))

;; Utility functions
(defn print-schema
  "Prints the GraphQL schema"
  []
  (pprint (schema/load-schema)))

;; REPL convenience
(defn go
  "Initialize and start the system"
  []
  (start)
  :ready)

(defn reset
  "Reset the system and reload code"
  []
  (stop)
  (refresh :after 'dev/go))

(println "Dev environment loaded. Type (go) to start the server, (reset) to reload code and restart.")
