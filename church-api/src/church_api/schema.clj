(ns church-api.schema
  (:require [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [church-api.db :as db]
            [clojure.tools.logging :as log]
            [clojure.string :as str]))

;; Helper functions for resolvers
(defn map-db-member
  "Converts database member record to GraphQL format"
  [member]
  (when member
    (-> member
        (update :id int)
        (clojure.set/rename-keys {:first_name :firstName
                                 :last_name :lastName
                                 :join_date :joinDate
                                 :created_at :createdAt
                                 :updated_at :updatedAt}))))

(defn map-db-event
  "Converts database event record to GraphQL format"
  [event]
  (when event
    (-> event
        (update :id int)
        (clojure.set/rename-keys {:event_date :eventDate
                                 :created_at :createdAt
                                 :updated_at :updatedAt}))))

(defn map-db-group
  "Converts database group record to GraphQL format"
  [group]
  (when group
    (-> group
        (update :id int)
        (update :leader_id #(when % (int %)))
        (clojure.set/rename-keys {:leader_id :leaderId
                                 :created_at :createdAt
                                 :updated_at :updatedAt}))))

;; GraphQL Resolvers
(def resolvers-map
  {:Query
   {:member (fn [_ args _]
              (map-db-member (db/get-member (:id args))))
    
    :members (fn [_ _ _]
               (map map-db-member (db/get-members)))
    
    :event (fn [_ args _]
             (map-db-event (db/get-event (:id args))))
    
    :events (fn [_ _ _]
              (map map-db-event (db/get-events)))
    
    :group (fn [_ args _]
             (map-db-group (db/get-group (:id args))))
    
    :groups (fn [_ _ _]
              (map map-db-group (db/get-groups)))}
   
   :Mutation
   {:createMember (fn [_ args _]
                    (let [member-data (-> args :input)
                          result (db/create-member! (-> member-data
                                                      (clojure.set/rename-keys {:firstName :first_name
                                                                              :lastName :last_name
                                                                              :joinDate :join_date})))]
                      (map-db-member (assoc member-data :id (get-in result [0 :id])))))
    
    :updateMember (fn [_ args _]
                    (let [id (:id args)
                          member-data (-> args :input)
                          updated-data (-> member-data
                                         (clojure.set/rename-keys {:firstName :first_name
                                                                 :lastName :last_name
                                                                 :joinDate :join_date}))]
                      (db/update-member! id updated-data)
                      (map-db-member (db/get-member id))))
    
    :deleteMember (fn [_ args _]
                    (let [id (:id args)
                          member (db/get-member id)]
                      (db/delete-member! id)
                      {:id id :success true}))
    
    :createEvent (fn [_ args _]
                   (let [event-data (-> args :input)
                         result (db/create-event! (-> event-data
                                                    (clojure.set/rename-keys {:eventDate :event_date})))]
                     (map-db-event (assoc event-data :id (get-in result [0 :id])))))
    
    :updateEvent (fn [_ args _]
                   (let [id (:id args)
                         event-data (-> args :input)
                         updated-data (-> event-data
                                        (clojure.set/rename-keys {:eventDate :event_date}))]
                     (db/update-event! id updated-data)
                     (map-db-event (db/get-event id))))
    
    :deleteEvent (fn [_ args _]
                   (let [id (:id args)]
                     (db/delete-event! id)
                     {:id id :success true}))
    
    :createGroup (fn [_ args _]
                   (let [group-data (-> args :input)
                         result (db/create-group! (-> group-data
                                                    (clojure.set/rename-keys {:leaderId :leader_id})))]
                     (map-db-group (assoc group-data :id (get-in result [0 :id])))))
    
    :updateGroup (fn [_ args _]
                   (let [id (:id args)
                         group-data (-> args :input)
                         updated-data (-> group-data
                                        (clojure.set/rename-keys {:leaderId :leader_id}))]
                     (db/update-group! id updated-data)
                     (map-db-group (db/get-group id))))
    
    :deleteGroup (fn [_ args _]
                   (let [id (:id args)]
                     (db/delete-group! id)
                     {:id id :success true}))
    
    :addMemberToGroup (fn [_ args _]
                        (let [member-id (:memberId args)
                              group-id (:groupId args)]
                          (db/add-member-to-group! member-id group-id)
                          {:memberId member-id
                           :groupId group-id
                           :success true}))
    
    :removeMemberFromGroup (fn [_ args _]
                             (let [member-id (:memberId args)
                                   group-id (:groupId args)]
                               (db/remove-member-from-group! member-id group-id)
                               {:memberId member-id
                                :groupId group-id
                                :success true}))
    
    :createAttendance (fn [_ args _]
                        (let [attendance-data (-> args :input)
                              result (db/create-attendance! attendance-data)]
                          (assoc attendance-data :id (get-in result [0 :id]))))
    
    :createDonation (fn [_ args _]
                      (let [donation-data (-> args :input)
                            result (db/create-donation! (-> donation-data
                                                          (clojure.set/rename-keys {:donationDate :donation_date})))]
                        (-> donation-data
                            (assoc :id (get-in result [0 :id]))
                            (update :memberId #(when % (int %))))))
    }
   
   :Member
   {:groups (fn [member _ _]
              (map map-db-group (db/get-member-groups (:id member))))
    
    :attendances (fn [member _ _]
                   (db/get-attendances-by-member (:id member)))
    
    :donations (fn [member _ _]
                 (db/get-donations-by-member (:id member)))}
   
   :Group
   {:leader (fn [group _ _]
              (when (:leaderId group)
                (map-db-member (db/get-member (:leaderId group)))))
    
    :members (fn [group _ _]
               (map map-db-member (db/get-group-members (:id group))))}
   
   :Event
   {:attendances (fn [event _ _]
                   (db/get-attendances-by-event (:id event)))}})

(def schema-edn
  {:objects
   {:Member
    {:description "A church member"
     :fields
     {:id {:type 'Int}
      :firstName {:type '(non-null String)}
      :lastName {:type '(non-null String)}
      :email {:type 'String}
      :phone {:type 'String}
      :address {:type 'String}
      :joinDate {:type 'String}
      :status {:type 'String}
      :createdAt {:type 'String}
      :updatedAt {:type 'String}
      :groups {:type '(list :Group)
               :description "Groups this member belongs to"}
      :attendances {:type '(list :Attendance)
                    :description "Events this member has attended"}
      :donations {:type '(list :Donation)
                  :description "Donations made by this member"}}}
    
    :Event
    {:description "A church event"
     :fields
     {:id {:type 'Int}
      :name {:type '(non-null String)}
      :description {:type 'String}
      :eventDate {:type '(non-null String)}
      :location {:type 'String}
      :createdAt {:type 'String}
      :updatedAt {:type 'String}
      :attendances {:type '(list :Attendance)
                    :description "Attendances for this event"}}}
    
    :Group
    {:description "A church group or ministry"
     :fields
     {:id {:type 'Int}
      :name {:type '(non-null String)}
      :description {:type 'String}
      :leaderId {:type 'Int}
      :leader {:type :Member
               :description "The leader of this group"}
      :members {:type '(list :Member)
                :description "Members of this group"}
      :createdAt {:type 'String}
      :updatedAt {:type 'String}}}
    
    :Attendance
    {:description "Record of a member attending an event"
     :fields
     {:id {:type 'Int}
      :memberId {:type '(non-null Int)}
      :eventId {:type '(non-null Int)}
      :attendedAt {:type 'String}
      :createdAt {:type 'String}}}
    
    :Donation
    {:description "A donation made to the church"
     :fields
     {:id {:type 'Int}
      :memberId {:type 'Int}
      :amount {:type '(non-null Float)}
      :donationDate {:type '(non-null String)}
      :purpose {:type 'String}
      :createdAt {:type 'String}}}
    
    :MemberGroupResult
    {:description "Result of a member-group operation"
     :fields
     {:memberId {:type 'Int}
      :groupId {:type 'Int}
      :success {:type 'Boolean}}}
    
    :DeleteResult
    {:description "Result of a delete operation"
     :fields
     {:id {:type 'Int}
      :success {:type 'Boolean}}}}
   
   :input-objects
   {:MemberInput
    {:fields
     {:firstName {:type '(non-null String)}
      :lastName {:type '(non-null String)}
      :email {:type 'String}
      :phone {:type 'String}
      :address {:type 'String}
      :joinDate {:type 'String}
      :status {:type 'String}}}
    
    :EventInput
    {:fields
     {:name {:type '(non-null String)}
      :description {:type 'String}
      :eventDate {:type '(non-null String)}
      :location {:type 'String}}}
    
    :GroupInput
    {:fields
     {:name {:type '(non-null String)}
      :description {:type 'String}
      :leaderId {:type 'Int}}}
    
    :AttendanceInput
    {:fields
     {:memberId {:type '(non-null Int)}
      :eventId {:type '(non-null Int)}}}
    
    :DonationInput
    {:fields
     {:memberId {:type 'Int}
      :amount {:type '(non-null Float)}
      :donationDate {:type '(non-null String)}
      :purpose {:type 'String}}}}
   
   :queries
   {:member
    {:type :Member
     :description "Get a member by ID"
     :args {:id {:type '(non-null Int)}}}
    
    :members
    {:type '(list :Member)
     :description "Get all members"}
    
    :event
    {:type :Event
     :description "Get an event by ID"
     :args {:id {:type '(non-null Int)}}}
    
    :events
    {:type '(list :Event)
     :description "Get all events"}
    
    :group
    {:type :Group
     :description "Get a group by ID"
     :args {:id {:type '(non-null Int)}}}
    
    :groups
    {:type '(list :Group)
     :description "Get all groups"}}
   
   :mutations
   {:createMember
    {:type :Member
     :description "Create a new member"
     :args {:input {:type :MemberInput}}}
    
    :updateMember
    {:type :Member
     :description "Update an existing member"
     :args {:id {:type '(non-null Int)}
            :input {:type :MemberInput}}}
    
    :deleteMember
    {:type :DeleteResult
     :description "Delete a member"
     :args {:id {:type '(non-null Int)}}}
    
    :createEvent
    {:type :Event
     :description "Create a new event"
     :args {:input {:type :EventInput}}}
    
    :updateEvent
    {:type :Event
     :description "Update an existing event"
     :args {:id {:type '(non-null Int)}
            :input {:type :EventInput}}}
    
    :deleteEvent
    {:type :DeleteResult
     :description "Delete an event"
     :args {:id {:type '(non-null Int)}}}
    
    :createGroup
    {:type :Group
     :description "Create a new group"
     :args {:input {:type :GroupInput}}}
    
    :updateGroup
    {:type :Group
     :description "Update an existing group"
     :args {:id {:type '(non-null Int)}
            :input {:type :GroupInput}}}
    
    :deleteGroup
    {:type :DeleteResult
     :description "Delete a group"
     :args {:id {:type '(non-null Int)}}}
    
    :addMemberToGroup
    {:type :MemberGroupResult
     :description "Add a member to a group"
     :args {:memberId {:type '(non-null Int)}
            :groupId {:type '(non-null Int)}}}
    
    :removeMemberFromGroup
    {:type :MemberGroupResult
     :description "Remove a member from a group"
     :args {:memberId {:type '(non-null Int)}
            :groupId {:type '(non-null Int)}}}
    
    :createAttendance
    {:type :Attendance
     :description "Record a member's attendance at an event"
     :args {:input {:type :AttendanceInput}}}
    
    :createDonation
    {:type :Donation
     :description "Record a donation"
     :args {:input {:type :DonationInput}}}}})

(defn load-schema
  "Loads the GraphQL schema"
  []
  schema-edn)
