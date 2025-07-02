(ns church-api.test-helper
  (:require [clojure.java.jdbc :as jdbc]
            [church-api.db :as db]
            [clojure.test :refer :all]
            [com.walmartlabs.lacinia :as lacinia]
            [church-api.schema :as schema]
            [clojure.walk :as walk]))

;; Test database spec - use in-memory SQLite for tests
(def test-db-spec
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     ":memory:"})

;; Replace the real db-spec with the test one for testing
(defn with-test-db [f]
  (with-redefs [db/db-spec test-db-spec]
    (db/init-db!)
    (f)))

;; Helper to execute GraphQL queries in tests
(defn execute-query
  ([query] (execute-query query nil))
  ([query variables]
   (let [compiled-schema (-> (schema/load-schema)
                            (com.walmartlabs.lacinia.schema/compile))]
     (lacinia/execute compiled-schema query variables nil))))

;; Sample test data
(def sample-member
  {:first_name "John"
   :last_name "Doe"
   :email "john.doe@example.com"
   :phone "555-123-4567"
   :address "123 Main St"
   :join_date "2023-01-01"
   :status "active"})

(def sample-event
  {:name "Sunday Service"
   :description "Weekly worship service"
   :event_date "2023-07-02T10:00:00"
   :location "Main Sanctuary"})

(def sample-group
  {:name "Youth Ministry"
   :description "Ministry for teenagers"})

;; Helper to insert test data
(defn insert-test-data []
  (jdbc/insert! db/db-spec :members sample-member)
  (jdbc/insert! db/db-spec :events sample-event)
  (jdbc/insert! db/db-spec :groups sample-group))

;; Helper to clean test data
(defn clean-test-data []
  (jdbc/delete! db/db-spec :member_groups [])
  (jdbc/delete! db/db-spec :attendances [])
  (jdbc/delete! db/db-spec :donations [])
  (jdbc/delete! db/db-spec :members [])
  (jdbc/delete! db/db-spec :events [])
  (jdbc/delete! db/db-spec :groups []))

;; Fixture for setting up and tearing down test data
(defn with-test-data [f]
  (clean-test-data)
  (insert-test-data)
  (f)
  (clean-test-data))

;; Helper to normalize GraphQL response for easier testing
(defn normalize-graphql-response [response]
  (walk/postwalk
   (fn [x]
     (if (and (map? x) (contains? x :id))
       (assoc x :id 1)  ; Normalize IDs for consistent testing
       x))
   response))
