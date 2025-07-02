(ns church-api.integration.group-test
  (:require [clojure.test :refer :all]
            [church-api.test-helper :as helper]
            [church-api.db :as db]))

(use-fixtures :once helper/with-test-db)
(use-fixtures :each helper/with-test-data)

(deftest test-query-groups
  (testing "Query all groups"
    (let [query "{ groups { id name description } }"
          result (helper/execute-query query)
          groups (get-in result [:data :groups])]
      (is (= 1 (count groups)))
      (is (= "Youth Ministry" (get-in groups [0 :name])))
      (is (= "Ministry for teenagers" (get-in groups [0 :description]))))))

(deftest test-query-group-by-id
  (testing "Query group by ID"
    (let [group-id 1
          query (str "{ group(id: " group-id ") { id name description } }")
          result (helper/execute-query query)
          group (get-in result [:data :group])]
      (is (= "Youth Ministry" (:name group)))
      (is (= "Ministry for teenagers" (:description group))))))

(deftest test-create-group
  (testing "Create a new group"
    (let [mutation "mutation CreateGroup($input: GroupInput!) {
                     createGroup(input: $input) {
                       id name description
                     }
                   }"
          variables {:input {:name "Choir"
                            :description "Church choir group"}}
          result (helper/execute-query mutation variables)
          created-group (get-in result [:data :createGroup])]
      (is (some? created-group))
      (is (= "Choir" (:name created-group)))
      (is (= "Church choir group" (:description created-group))))))

(deftest test-update-group
  (testing "Update an existing group"
    (let [group-id 1
          mutation (str "mutation UpdateGroup($id: Int!, $input: GroupInput!) {
                        updateGroup(id: $id, input: $input) {
                          id name description
                        }
                      }")
          variables {:id group-id
                    :input {:name "Youth Ministry"
                           :description "Updated ministry for young people"}}
          result (helper/execute-query mutation variables)
          updated-group (get-in result [:data :updateGroup])]
      (is (some? updated-group))
      (is (= "Youth Ministry" (:name updated-group)))
      (is (= "Updated ministry for young people" (:description updated-group))))))

(deftest test-delete-group
  (testing "Delete a group"
    (let [group-id 1
          mutation (str "mutation DeleteGroup($id: Int!) {
                        deleteGroup(id: $id) {
                          id success
                        }
                      }")
          variables {:id group-id}
          result (helper/execute-query mutation variables)
          delete-result (get-in result [:data :deleteGroup])]
      (is (some? delete-result))
      (is (:success delete-result))
      (is (= group-id (:id delete-result)))
      
      ;; Verify group is deleted
      (let [query (str "{ group(id: " group-id ") { id } }")
            result (helper/execute-query query)
            group (get-in result [:data :group])]
        (is (nil? group))))))

(deftest test-add-member-to-group
  (testing "Add a member to a group"
    (let [member-id 1
          group-id 1
          mutation (str "mutation AddMemberToGroup($memberId: Int!, $groupId: Int!) {
                        addMemberToGroup(memberId: $memberId, groupId: $groupId) {
                          memberId groupId success
                        }
                      }")
          variables {:memberId member-id
                    :groupId group-id}
          result (helper/execute-query mutation variables)
          add-result (get-in result [:data :addMemberToGroup])]
      (is (some? add-result))
      (is (:success add-result))
      (is (= member-id (:memberId add-result)))
      (is (= group-id (:groupId add-result)))
      
      ;; Verify member is in group
      (let [query (str "{ group(id: " group-id ") { members { id firstName lastName } } }")
            result (helper/execute-query query)
            members (get-in result [:data :group :members])]
        (is (= 1 (count members)))
        (is (= "John" (get-in members [0 :firstName])))))))

(deftest test-remove-member-from-group
  (testing "Remove a member from a group"
    ;; First add the member to the group
    (db/add-member-to-group! 1 1)
    
    ;; Then test removal
    (let [member-id 1
          group-id 1
          mutation (str "mutation RemoveMemberFromGroup($memberId: Int!, $groupId: Int!) {
                        removeMemberFromGroup(memberId: $memberId, groupId: $groupId) {
                          memberId groupId success
                        }
                      }")
          variables {:memberId member-id
                    :groupId group-id}
          result (helper/execute-query mutation variables)
          remove-result (get-in result [:data :removeMemberFromGroup])]
      (is (some? remove-result))
      (is (:success remove-result))
      
      ;; Verify member is not in group
      (let [query (str "{ group(id: " group-id ") { members { id } } }")
            result (helper/execute-query query)
            members (get-in result [:data :group :members])]
        (is (empty? members))))))
