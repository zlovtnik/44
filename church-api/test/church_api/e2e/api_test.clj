(ns church-api.e2e.api-test
  (:require [clojure.test :refer :all]
            [io.pedestal.http :as http]
            [io.pedestal.test :as test]
            [church-api.core :as core]
            [church-api.test-helper :as helper]
            [clojure.data.json :as json]))

(use-fixtures :once helper/with-test-db)
(use-fixtures :each helper/with-test-data)

(defn make-graphql-request
  "Makes a GraphQL request to the test service"
  [service query variables]
  (let [request-body (json/write-str {:query query :variables variables})
        response (test/response-for service
                                    :post "/graphql"
                                    :headers {"Content-Type" "application/json"}
                                    :body request-body)]
    {:status (:status response)
     :body (json/read-str (:body response) :key-fn keyword)}))

(deftest test-e2e-member-queries
  (testing "End-to-end member queries"
    (let [service (::http/service-fn (core/create-server))]
      
      ;; Query all members
      (let [query "{ members { id firstName lastName email } }"
            response (make-graphql-request service query nil)]
        (is (= 200 (:status response)))
        (let [members (get-in response [:body :data :members])]
          (is (= 1 (count members)))
          (is (= "John" (get-in members [0 :firstName])))
          (is (= "Doe" (get-in members [0 :lastName])))))
      
      ;; Query member by ID
      (let [query "{ member(id: 1) { id firstName lastName email } }"
            response (make-graphql-request service query nil)]
        (is (= 200 (:status response)))
        (let [member (get-in response [:body :data :member])]
          (is (= "John" (:firstName member)))
          (is (= "Doe" (:lastName member))))))))

(deftest test-e2e-member-mutations
  (testing "End-to-end member mutations"
    (let [service (::http/service-fn (core/create-server))]
      
      ;; Create a new member
      (let [mutation "mutation CreateMember($input: MemberInput!) {
                       createMember(input: $input) {
                         id firstName lastName email
                       }
                     }"
            variables {:input {:firstName "Jane"
                              :lastName "Smith"
                              :email "jane.smith@example.com"
                              :phone "555-987-6543"
                              :status "active"}}
            response (make-graphql-request service mutation variables)]
        (is (= 200 (:status response)))
        (let [created-member (get-in response [:body :data :createMember])]
          (is (some? created-member))
          (is (= "Jane" (:firstName created-member)))
          (is (= "Smith" (:lastName created-member)))))
      
      ;; Update a member
      (let [mutation "mutation UpdateMember($id: Int!, $input: MemberInput!) {
                       updateMember(id: $id, input: $input) {
                         id firstName lastName email
                       }
                     }"
            variables {:id 1
                      :input {:firstName "John"
                             :lastName "Updated"
                             :email "john.updated@example.com"
                             :phone "555-123-4567"
                             :status "active"}}
            response (make-graphql-request service mutation variables)]
        (is (= 200 (:status response)))
        (let [updated-member (get-in response [:body :data :updateMember])]
          (is (some? updated-member))
          (is (= "John" (:firstName updated-member)))
          (is (= "Updated" (:lastName updated-member)))))
      
      ;; Delete a member
      (let [mutation "mutation DeleteMember($id: Int!) {
                       deleteMember(id: $id) {
                         id success
                       }
                     }"
            variables {:id 1}
            response (make-graphql-request service mutation variables)]
        (is (= 200 (:status response)))
        (let [delete-result (get-in response [:body :data :deleteMember])]
          (is (some? delete-result))
          (is (:success delete-result)))))))

(deftest test-e2e-event-operations
  (testing "End-to-end event operations"
    (let [service (::http/service-fn (core/create-server))]
      
      ;; Query all events
      (let [query "{ events { id name description eventDate location } }"
            response (make-graphql-request service query nil)]
        (is (= 200 (:status response)))
        (let [events (get-in response [:body :data :events])]
          (is (= 1 (count events)))
          (is (= "Sunday Service" (get-in events [0 :name])))))
      
      ;; Create a new event
      (let [mutation "mutation CreateEvent($input: EventInput!) {
                       createEvent(input: $input) {
                         id name description eventDate location
                       }
                     }"
            variables {:input {:name "Bible Study"
                              :description "Weekly Bible study group"
                              :eventDate "2023-07-05T19:00:00"
                              :location "Fellowship Hall"}}
            response (make-graphql-request service mutation variables)]
        (is (= 200 (:status response)))
        (let [created-event (get-in response [:body :data :createEvent])]
          (is (some? created-event))
          (is (= "Bible Study" (:name created-event)))
          (is (= "Fellowship Hall" (:location created-event))))))))

(deftest test-e2e-group-operations
  (testing "End-to-end group operations"
    (let [service (::http/service-fn (core/create-server))]
      
      ;; Query all groups
      (let [query "{ groups { id name description } }"
            response (make-graphql-request service query nil)]
        (is (= 200 (:status response)))
        (let [groups (get-in response [:body :data :groups])]
          (is (= 1 (count groups)))
          (is (= "Youth Ministry" (get-in groups [0 :name])))))
      
      ;; Add member to group
      (let [mutation "mutation AddMemberToGroup($memberId: Int!, $groupId: Int!) {
                       addMemberToGroup(memberId: $memberId, groupId: $groupId) {
                         memberId groupId success
                       }
                     }"
            variables {:memberId 1
                      :groupId 1}
            response (make-graphql-request service mutation variables)]
        (is (= 200 (:status response)))
        (let [add-result (get-in response [:body :data :addMemberToGroup])]
          (is (some? add-result))
          (is (:success add-result))))
      
      ;; Query group with members
      (let [query "{ group(id: 1) { id name members { id firstName lastName } } }"
            response (make-graphql-request service query nil)]
        (is (= 200 (:status response)))
        (let [members (get-in response [:body :data :group :members])]
          (is (= 1 (count members)))
          (is (= "John" (get-in members [0 :firstName]))))))))
