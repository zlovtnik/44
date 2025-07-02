(ns church-api.integration.event-test
  (:require [clojure.test :refer :all]
            [church-api.test-helper :as helper]
            [church-api.db :as db]))

(use-fixtures :once helper/with-test-db)
(use-fixtures :each helper/with-test-data)

(deftest test-query-events
  (testing "Query all events"
    (let [query "{ events { id name description eventDate location } }"
          result (helper/execute-query query)
          events (get-in result [:data :events])]
      (is (= 1 (count events)))
      (is (= "Sunday Service" (get-in events [0 :name])))
      (is (= "Weekly worship service" (get-in events [0 :description])))
      (is (= "Main Sanctuary" (get-in events [0 :location]))))))

(deftest test-query-event-by-id
  (testing "Query event by ID"
    (let [event-id 1
          query (str "{ event(id: " event-id ") { id name description eventDate location } }")
          result (helper/execute-query query)
          event (get-in result [:data :event])]
      (is (= "Sunday Service" (:name event)))
      (is (= "Weekly worship service" (:description event)))
      (is (= "Main Sanctuary" (:location event))))))

(deftest test-create-event
  (testing "Create a new event"
    (let [mutation "mutation CreateEvent($input: EventInput!) {
                     createEvent(input: $input) {
                       id name description eventDate location
                     }
                   }"
          variables {:input {:name "Bible Study"
                            :description "Weekly Bible study group"
                            :eventDate "2023-07-05T19:00:00"
                            :location "Fellowship Hall"}}
          result (helper/execute-query mutation variables)
          created-event (get-in result [:data :createEvent])]
      (is (some? created-event))
      (is (= "Bible Study" (:name created-event)))
      (is (= "Weekly Bible study group" (:description created-event)))
      (is (= "Fellowship Hall" (:location created-event))))))

(deftest test-update-event
  (testing "Update an existing event"
    (let [event-id 1
          mutation (str "mutation UpdateEvent($id: Int!, $input: EventInput!) {
                        updateEvent(id: $id, input: $input) {
                          id name description eventDate location
                        }
                      }")
          variables {:id event-id
                    :input {:name "Sunday Service"
                           :description "Updated weekly worship service"
                           :eventDate "2023-07-02T10:00:00"
                           :location "Main Auditorium"}}
          result (helper/execute-query mutation variables)
          updated-event (get-in result [:data :updateEvent])]
      (is (some? updated-event))
      (is (= "Sunday Service" (:name updated-event)))
      (is (= "Updated weekly worship service" (:description updated-event)))
      (is (= "Main Auditorium" (:location updated-event))))))

(deftest test-delete-event
  (testing "Delete an event"
    (let [event-id 1
          mutation (str "mutation DeleteEvent($id: Int!) {
                        deleteEvent(id: $id) {
                          id success
                        }
                      }")
          variables {:id event-id}
          result (helper/execute-query mutation variables)
          delete-result (get-in result [:data :deleteEvent])]
      (is (some? delete-result))
      (is (:success delete-result))
      (is (= event-id (:id delete-result)))
      
      ;; Verify event is deleted
      (let [query (str "{ event(id: " event-id ") { id } }")
            result (helper/execute-query query)
            event (get-in result [:data :event])]
        (is (nil? event))))))
