(ns church-api.core
  (:require [io.pedestal.http :as http]
            [com.walmartlabs.lacinia.pedestal :as lacinia-pedestal]
            [com.walmartlabs.lacinia.schema :as schema]
            [church-api.schema :as church-schema]
            [church-api.db :as db]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pprint])
  (:gen-class))

(defn load-schema
  "Loads the compiled GraphQL schema."
  []
  (try
    (log/info "Loading GraphQL schema...")
    (let [raw-schema (church-schema/load-schema)
          _ (log/debug "Raw schema loaded:")
          _ (log/debug (with-out-str (pprint/pprint raw-schema)))
          _ (log/debug "Compiling schema...")
          compiled-schema (schema/compile raw-schema)
          _ (log/debug "Compiled schema:")
          _ (log/debug (with-out-str (pprint/pprint compiled-schema)))]
      (log/info "Schema compiled successfully")
      compiled-schema)
    (catch Exception e
      (log/error e "Failed to load or compile schema")
      (log/error "Exception details:" (ex-data e))
      (throw e))))

(defn create-server
  "Creates a Pedestal server with the GraphQL schema."
  []
  (try
    (log/info "Creating Pedestal server...")
    (let [_ (log/debug "Loading schema...")
          compiled-schema (load-schema)
          _ (log/debug "Schema loaded successfully")
          opts {:graphiql true
                :port 8888
                :host "0.0.0.0"}
          _ (log/debug "Creating service map with options:" opts)
          service-map (lacinia-pedestal/service-map compiled-schema opts)
          _ (log/debug "Service map created:")
          _ (log/debug (with-out-str (pprint/pprint service-map)))
          _ (log/debug "Creating HTTP server...")
          server (http/create-server service-map)]
      (log/info "Server created successfully")
      server)
    (catch Exception e
      (log/error e "Failed to create server")
      (log/error "Exception details:" (ex-data e))
      (throw e))))

(defn start-server
  "Starts the Pedestal server."
  [server]
  (try
    (log/info "Starting server on port 8888...")
    (log/info "Initializing database...")
    (try
      (db/init-db!) ; Initialize the database
      (log/info "Database initialized successfully")
      (catch Exception e
        (log/error e "Failed to initialize database")
        (log/error "Database error details:" (ex-data e))
        (throw e)))
    (log/debug "Starting HTTP server...")
    (let [started-server (http/start server)]
      (log/info "Server started successfully on port 8888")
      (log/info "GraphiQL interface available at http://localhost:8888/")
      started-server)
    (catch Exception e
      (log/error e "Failed to start server")
      (log/error "Server error details:" (ex-data e))
      (throw e))))

(defn -main [& _]
  (try
    (log/info "Starting church-api...")
    (log/debug "JVM version:" (System/getProperty "java.version"))
    (log/debug "Logback config:" (System/getProperty "logback.configurationFile"))
    (let [server (create-server)]
      (start-server server)
      (println "Server started. GraphiQL available at http://localhost:8888/graphiql")
      @(promise)) ; Keep the application running
    (catch Exception e
      (log/error e "Fatal error during startup")
      (log/error "Error details:" (ex-data e))
      (System/exit 1))))
