(ns church-api.unit.db-test
  (:require [clojure.test :refer :all]
            [church-api.test-helper :as helper]
            [church-api.db :as db]))

(use-fixtures :once helper/with-test-db)
(use-fixtures :each helper/with-test-data)

(deftest test-member-crud
  (testing "Member CRUD operations"
    ;; Create
    (let [new-member {:first_name "Alice"
                     :last_name "Johnson"
                     :email "alice.johnson@example.com"
                     :phone "555-111-2222"
                     :status "active"}
          result (db/create-member! new-member)
          id (get-in result [0 :id])]
      
      ;; Read
      (let [member (db/get-member id)]
        (is (some? member))
        (is (= "Alice" (:first_name member)))
        (is (= "Johnson" (:last_name member)))
        (is (= "alice.johnson@example.com" (:email member))))
      
      ;; Update
      (let [updated-data {:first_name "Alice"
                         :last_name "Smith"
                         :email "alice.smith@example.com"}
            _ (db/update-member! id updated-data)
            updated-member (db/get-member id)]
        (is (= "Alice" (:first_name updated-member)))
        (is (= "Smith" (:last_name updated-member)))
        (is (= "alice.smith@example.com" (:email updated-member))))
      
      ;; Delete
      (db/delete-member! id)
      (is (nil? (db/get-member id))))))

(deftest test-event-crud
  (testing "Event CRUD operations"
    ;; Create
    (let [new-event {:name "Prayer Meeting"
                    :description "Weekly prayer gathering"
                    :event_date "2023-07-06T18:30:00"
                    :location "Prayer Room"}
          result (db/create-event! new-event)
          id (get-in result [0 :id])]
      
      ;; Read
      (let [event (db/get-event id)]
        (is (some? event))
        (is (= "Prayer Meeting" (:name event)))
        (is (= "Weekly prayer gathering" (:description event)))
        (is (= "Prayer Room" (:location event))))
      
      ;; Update
      (let [updated-data {:name "Prayer Meeting"
                         :description "Updated weekly prayer gathering"
                         :location "Chapel"}
            _ (db/update-event! id updated-data)
            updated-event (db/get-event id)]
        (is (= "Prayer Meeting" (:name updated-event)))
        (is (= "Updated weekly prayer gathering" (:description updated-event)))
        (is (= "Chapel" (:location updated-event))))
      
      ;; Delete
      (db/delete-event! id)
      (is (nil? (db/get-event id))))))

(deftest test-group-crud
  (testing "Group CRUD operations"
    ;; Create
    (let [new-group {:name "Men's Ministry"
                    :description "Ministry for men"}
          result (db/create-group! new-group)
          id (get-in result [0 :id])]
      
      ;; Read
      (let [group (db/get-group id)]
        (is (some? group))
        (is (= "Men's Ministry" (:name group)))
        (is (= "Ministry for men" (:description group))))
      
      ;; Update
      (let [updated-data {:name "Men's Ministry"
                         :description "Updated ministry for men"}
            _ (db/update-group! id updated-data)
            updated-group (db/get-group id)]
        (is (= "Men's Ministry" (:name updated-group)))
        (is (= "Updated ministry for men" (:description updated-group))))
      
      ;; Delete
      (db/delete-group! id)
      (is (nil? (db/get-group id))))))

(deftest test-attendance-operations
  (testing "Attendance operations"
    (let [attendance {:member_id 1
                     :event_id 1}
          result (db/create-attendance! attendance)
          id (get-in result [0 :id])]
      
      ;; Read
      (let [attendance (db/get-attendance id)]
        (is (some? attendance))
        (is (= 1 (:member_id attendance)))
        (is (= 1 (:event_id attendance))))
      
      ;; Get by event
      (let [attendances (db/get-attendances-by-event 1)]
        (is (= 1 (count attendances)))
        (is (= 1 (:member_id (first attendances)))))
      
      ;; Get by member
      (let [attendances (db/get-attendances-by-member 1)]
        (is (= 1 (count attendances)))
        (is (= 1 (:event_id (first attendances))))))))

(deftest test-donation-operations
  (testing "Donation operations"
    (let [donation {:member_id 1
                   :amount 100.0
                   :donation_date "2023-07-01"
                   :purpose "Tithe"}
          result (db/create-donation! donation)
          id (get-in result [0 :id])]
      
      ;; Read
      (let [donation (db/get-donation id)]
        (is (some? donation))
        (is (= 1 (:member_id donation)))
        (is (= 100.0 (:amount donation)))
        (is (= "Tithe" (:purpose donation))))
      
      ;; Get by member
      (let [donations (db/get-donations-by-member 1)]
        (is (= 1 (count donations)))
        (is (= 100.0 (:amount (first donations))))))))

(deftest test-member-group-relationships
  (testing "Member-Group relationship operations"
    ;; Add member to group
    (db/add-member-to-group! 1 1)
    
    ;; Get group members
    (let [members (db/get-group-members 1)]
      (is (= 1 (count members)))
      (is (= "John" (:first_name (first members)))))
    
    ;; Get member groups
    (let [groups (db/get-member-groups 1)]
      (is (= 1 (count groups)))
      (is (= "Youth Ministry" (:name (first groups)))))
    
    ;; Remove member from group
    (db/remove-member-from-group! 1 1)
    
    ;; Verify removal
    (let [members (db/get-group-members 1)]
      (is (empty? members)))))
