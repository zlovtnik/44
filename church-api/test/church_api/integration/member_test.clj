(ns church-api.integration.member-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [church-api.test-helper :as helper]))

(use-fixtures :once helper/with-test-db)
(use-fixtures :each helper/with-test-data)

(deftest test-query-members
  (testing "Query all members"
    (let [query "{ members { id firstName lastName email } }"
          result (helper/execute-query query)
          members (get-in result [:data :members])]
      (is (= 1 (count members)))
      (is (= "John" (get-in members [0 :firstName])))
      (is (= "Doe" (get-in members [0 :lastName]))))))

(deftest test-query-member-by-id
  (testing "Query member by ID"
    (let [member-id 1
          query (str "{ member(id: " member-id ") { id firstName lastName email } }")
          result (helper/execute-query query)
          member (get-in result [:data :member])]
      (is (= "John" (:firstName member)))
      (is (= "Doe" (:lastName member))))))

(deftest test-create-member
  (testing "Create a new member"
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
          result (helper/execute-query mutation variables)
          created-member (get-in result [:data :createMember])]
      (is (some? created-member))
      (is (= "Jane" (:firstName created-member)))
      (is (= "Smith" (:lastName created-member)))
      (is (= "jane.smith@example.com" (:email created-member))))))

(deftest test-update-member
  (testing "Update an existing member"
    (let [member-id 1
          mutation (str "mutation UpdateMember($id: Int!, $input: MemberInput!) {
                        updateMember(id: $id, input: $input) {
                          id firstName lastName email
                        }
                      }")
          variables {:id member-id
                    :input {:firstName "John"
                           :lastName "Updated"
                           :email "john.updated@example.com"
                           :phone "555-123-4567"
                           :status "active"}}
          result (helper/execute-query mutation variables)
          updated-member (get-in result [:data :updateMember])]
      (is (some? updated-member))
      (is (= "John" (:firstName updated-member)))
      (is (= "Updated" (:lastName updated-member)))
      (is (= "john.updated@example.com" (:email updated-member))))))

(deftest test-delete-member
  (testing "Delete a member"
    (let [member-id 1
          mutation (str "mutation DeleteMember($id: Int!) {
                        deleteMember(id: $id) {
                          id success
                        }
                      }")
          variables {:id member-id}
          result (helper/execute-query mutation variables)
          delete-result (get-in result [:data :deleteMember])]
      (is (some? delete-result))
      (is (:success delete-result))
      (is (= member-id (:id delete-result)))
      
      ;; Verify member is deleted
      (let [query (str "{ member(id: " member-id ") { id } }")
            result (helper/execute-query query)
            member (get-in result [:data :member])]
        (is (nil? member))))))
