(ns church-api.core
  (:require [io.pedestal.http :as http]
            [com.walmartlabs.lacinia.pedestal :as lacinia-pedestal]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [church-api.schema :as church-schema]
            [church-api.db :as db]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:gen-class))

(defn load-schema
  "Loads the compiled GraphQL schema."
  []
  (-> (church-schema/load-schema)
      (util/attach-resolvers church-schema/resolvers-map)
      schema/compile))

(defn create-server
  "Creates a Pedestal server with the GraphQL schema."
  []
  (let [compiled-schema (load-schema)
        opts {:graphiql true
              :port 8888
              :host "0.0.0.0"}]
    (-> compiled-schema
        (lacinia-pedestal/service-map opts)
        http/create-server)))

(defn start-server
  "Starts the Pedestal server."
  [server]
  (println "Starting server on port 8888...")
  (db/init-db!) ; Initialize the database
  (http/start server))

(defn -main
  "Main entry point for the application."
  [& args]
  (-> (create-server)
      start-server)
  (println "Server started. GraphiQL available at http://localhost:8888/graphiql")
  @(promise)) ; Keep the application running
