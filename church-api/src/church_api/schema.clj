(ns church-api.schema
  (:require [com.walmartlabs.lacinia.schema :as s]
            [com.walmartlabs.lacinia.util :as util]
            [church-api.db :as db]
            [clojure.set :as set]
            [clojure.tools.logging :as log]))

;; Helper functions for resolvers
(defn map-db-member
  "Converts database member record to GraphQL format"
  [member]
  (when member
    (-> member
        (update :id int)
        (set/rename-keys {:first_name :firstName
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
        (set/rename-keys {:event_date :eventDate
                          :created_at :createdAt
                          :updated_at :updatedAt}))))

(defn map-db-group
  "Converts database group record to GraphQL format"
  [group]
  (when group
    (-> group
        (update :id int)
        (update :leader_id #(when % (int %)))
        (set/rename-keys {:leader_id :leaderId
                          :created_at :createdAt
                          :updated_at :updatedAt}))))

(defn map-db-attendance
  "Converts database attendance record to GraphQL format"
  [attendance]
  (when attendance
    (-> attendance
        (update :id int)
        (update :member_id int)
        (update :event_id int)
        (set/rename-keys {:member_id :memberId
                          :event_id :eventId
                          :attended_at :attendedAt
                          :created_at :createdAt}))))

(defn map-db-donation
  "Converts database donation record to GraphQL format"
  [donation]
  (when donation
    (-> donation
        (update :id int)
        (update :member_id int)
        (update :amount bigdec)
        (set/rename-keys {:member_id :memberId
                          :donation_date :donationDate
                          :created_at :createdAt}))))

;; GraphQL Resolvers
(def field-resolvers
  {:Member
   {:groups (fn [_ {:keys [id]} _]
              (map map-db-group (db/get-member-groups id)))
    :attendances (fn [_ {:keys [id]} _]
                   (map map-db-attendance (db/get-member-attendances id)))
    :donations (fn [_ {:keys [id]} _]
                 (map map-db-donation (db/get-member-donations id)))}
   :Group
   {:leader (fn [{:keys [leaderId]} _ _]
              (map-db-member (db/get-member leaderId)))
    :members (fn [{:keys [id]} _ _]
               (map map-db-member (db/get-group-members id)))}
   :Event
   {:attendances (fn [{:keys [id]} _ _]
                   (map map-db-attendance (db/get-event-attendances id)))}})

(def query-resolvers
  {:member (fn [_ {:keys [id]} _]
             (map-db-member (db/get-member id)))
   :members (fn [_ _ _]
              (map map-db-member (db/get-members)))
   :event (fn [_ {:keys [id]} _]
            (map-db-event (db/get-event id)))
   :events (fn [_ _ _]
             (map map-db-event (db/get-events)))
   :group (fn [_ {:keys [id]} _]
            (map-db-group (db/get-group id)))
   :groups (fn [_ _ _]
             (map map-db-group (db/get-groups)))})

(def mutation-resolvers
  {:createMember (fn [_ {:keys [input]} _]
                   (let [result (db/create-member! (-> input
                                                       (set/rename-keys {:firstName :first_name
                                                                         :lastName :last_name
                                                                         :joinDate :join_date})))]
                     (map-db-member (db/get-member (get-in result [0 :id])))))
   :updateMember (fn [_ {:keys [id input]} _]
                   (db/update-member! id (-> input
                                             (set/rename-keys {:firstName :first_name
                                                               :lastName :last_name
                                                               :joinDate :join_date})))
                   (map-db-member (db/get-member id)))
   :createAttendance (fn [_ {:keys [input]} _]
                       (let [result (db/create-attendance! input)]
                         (map-db-attendance (db/get-attendance (get-in result [0 :id])))))
   :createDonation (fn [_ {:keys [input]} _]
                     (let [result (db/create-donation! (-> input
                                                           (set/rename-keys {:donationDate :donation_date})))]
                       (map-db-donation (db/get-donation (get-in result [0 :id])))))
   :addMemberToGroup (fn [_ {:keys [memberId groupId]} _]
                       (db/add-member-to-group! memberId groupId)
                       {:memberId memberId :groupId groupId :success true})
   :removeMemberFromGroup (fn [_ {:keys [memberId groupId]} _]
                            (db/remove-member-from-group! memberId groupId)
                            {:memberId memberId :groupId groupId :success true})
   :deleteMember (fn [_ {:keys [id]} _]
                   (db/delete-member! id)
                   {:id id :success true})
   :createEvent (fn [_ {:keys [input]} _]
                  (let [result (db/create-event! (-> input
                                                     (set/rename-keys {:eventDate :event_date})))]
                    (map-db-event (db/get-event (get-in result [0 :id])))))
   :updateEvent (fn [_ {:keys [id input]} _]
                  (db/update-event! id (-> input
                                           (set/rename-keys {:eventDate :event_date})))
                  (map-db-event (db/get-event id)))
   :deleteEvent (fn [_ {:keys [id]} _]
                  (db/delete-event! id)
                  {:id id :success true})
   :createGroup (fn [_ {:keys [input]} _]
                  (let [result (db/create-group! (-> input
                                                     (set/rename-keys {:leaderId :leader_id})))]
                    (map-db-group (db/get-group (get-in result [0 :id])))))})

(def resolvers-map
  (merge
   {:Query query-resolvers
    :Mutation mutation-resolvers}
   field-resolvers))

(def schema-edn
  {:objects
   {:Member
    {:description "A church member",
     :fields
     {:id {:type '(non-null Int)},
      :firstName {:type '(non-null String)},
      :lastName {:type '(non-null String)},
      :email {:type 'String},
      :phone {:type 'String},
      :address {:type 'String},
      :joinDate {:type 'String},
      :status {:type 'String},
      :groups {:type '(list :Group)},
      :attendances {:type '(list :Attendance)},
      :donations {:type '(list :Donation)}}},
    :Group
    {:description "A group within the church",
     :fields
     {:id {:type 'Int},
      :name {:type '(non-null String)},
      :description {:type 'String},
      :createdAt {:type 'String'}}},
    :Attendance
    {:description "Record of a member attending an event",
     :fields
     {:id {:type 'Int},
      :memberId {:type '(non-null Int)},
      :eventId {:type '(non-null Int)},
      :attendedAt {:type 'String},
      :createdAt {:type 'String'}}},
    :Donation
    {:description "A donation made to the church",
     :fields
     {:id {:type 'Int},
      :memberId {:type 'Int},
      :amount {:type '(non-null Float)},
      :donationDate {:type '(non-null String)},
      :purpose {:type 'String},
      :createdAt {:type 'String'}}},
    :MemberGroupResult
    {:description "Result of a member-group operation",
     :fields
     {:memberId {:type 'Int},
      :groupId {:type 'Int},
      :success {:type 'Boolean'}}},
    :DeleteResult
    {:description "Result of a delete operation",
     :fields {:id {:type 'Int}, :success {:type 'Boolean'}}}},
   :input-objects
   {:MemberInput
    {:fields
     {:firstName {:type '(non-null String)},
      :lastName {:type '(non-null String)},
      :email {:type 'String},
      :phone {:type 'String},
      :address {:type 'String},
      :joinDate {:type 'String},
      :status {:type 'String'}}},
    :GroupInput
    {:fields
     {:name {:type '(non-null String)},
      :description {:type 'String},
      :leaderId {:type 'Int'}}},
    :EventInput
    {:fields
     {:name {:type '(non-null String)},
      :description {:type 'String},
      :eventDate {:type 'String},
      :location {:type 'String'}}},
    :AttendanceInput
    {:fields
     {:memberId {:type '(non-null Int)},
      :eventId {:type '(non-null Int)}}},
    :DonationInput
    {:fields
     {:memberId {:type '(non-null Int)},
      :amount {:type '(non-null Float)},
      :donationDate {:type 'String},
      :purpose {:type 'String'}}}},
   :queries
   {:member
    {:type :Member,
     :description "Get a member by ID",
     :args {:id {:type '(non-null Int)}},
     :resolve :member},
    :members
    {:type '(list :Member),
     :description "Get all members",
     :resolve :members},
    :event
    {:type :Event,
     :description "Get an event by ID",
     :args {:id {:type '(non-null Int)}},
     :resolve :event},
    :events
    {:type '(list :Event),
     :description "Get all events",
     :resolve :events},
    :group
    {:type :Group,
     :description "Get a group by ID",
     :args {:id {:type '(non-null Int)}},
     :resolve :group},
    :groups
    {:type '(list :Group),
     :description "Get all groups",
     :resolve :groups},
    :attendanceById
    {:type :Attendance,
     :description "Get an attendance record by ID",
     :args {:id {:type '(non-null Int)}},
     :resolve :attendance-by-id},
    :attendancesByEvent
    {:type '(list :Attendance),
     :description "Get all attendances for a specific event",
     :args {:eventId {:type '(non-null Int)}},
     :resolve :attendances-by-event},
    :attendancesByMember
    {:type '(list :Attendance),
     :description "Get all attendances for a specific member",
     :args {:memberId {:type '(non-null Int)}},
     :resolve :attendances-by-member},
    :donationById
    {:type :Donation,
     :description "Get a donation by ID",
     :args {:id {:type '(non-null Int)}},
     :resolve :donation-by-id},
    :donationsByMember
    {:type '(list :Donation),
     :description "Get all donations for a specific member",
     :args {:memberId {:type '(non-null Int)}},
     :resolve :donations-by-member},
    :groupMembers
    {:type '(list :Member),
     :description "Get all members in a specific group",
     :args {:groupId {:type '(non-null Int)}},
     :resolve :group-members},
    :memberGroups
    {:type '(list :Group),
     :description "Get all groups for a specific member",
     :args {:memberId {:type '(non-null Int)}},
     :resolve :member-groups}},
   :mutations
   {:createMember
    {:type :Member,
     :description "Create a new member",
     :args {:input {:type :MemberInput}},
     :resolve :createMember},
    :updateMember
    {:type :Member,
     :description "Update an existing member",
     :args {:id {:type '(non-null Int)}, :input {:type :MemberInput}},
     :resolve :updateMember},
    :deleteMember
    {:type :DeleteResult,
     :description "Delete a member",
     :args {:id {:type '(non-null Int)}},
     :resolve :deleteMember},
    :createEvent
    {:type :Event,
     :description "Create a new event",
     :args {:input {:type :EventInput}},
     :resolve :createEvent},
    :updateEvent
    {:type :Event,
     :description "Update an existing event",
     :args {:id {:type '(non-null Int)}, :input {:type :EventInput}},
     :resolve :updateEvent},
    :deleteEvent
    {:type :DeleteResult,
     :description "Delete an event",
     :args {:id {:type '(non-null Int)}},
     :resolve :deleteEvent},
    :createGroup
    {:type :Group,
     :description "Create a new group",
     :args {:input {:type :GroupInput}},
     :resolve :createGroup},
    :updateGroup
    {:type :Group,
     :description "Update an existing group",
     :args {:id {:type '(non-null Int)}, :input {:type :GroupInput}},
     :resolve :updateGroup},
    :deleteGroup
    {:type :DeleteResult,
     :description "Delete a group",
     :args {:id {:type '(non-null Int)}},
     :resolve :deleteGroup},
    :addMemberToGroup
    {:type :MemberGroupResult,
     :description "Add a member to a group",
     :args {:memberId {:type '(non-null Int)}, :groupId {:type '(non-null Int)}},
     :resolve :addMemberToGroup},
    :removeMemberFromGroup
    {:type :MemberGroupResult,
     :description "Remove a member from a group",
     :args {:memberId {:type '(non-null Int)}, :groupId {:type '(non-null Int)}},
     :resolve :removeMemberFromGroup},
    :createAttendance
    {:type :Attendance,
     :description "Record a member's attendance at an event",
     :args {:input {:type :AttendanceInput}},
     :resolve :createAttendance},
    :createDonation
    {:type :Donation,
     :description "Record a donation",
     :args {:input {:type :DonationInput}},
     :resolve :createDonation}}})

;; Define scalar transformers for custom types
(def scalar-transformers
  {:DateTime {:parse str
              :serialize str}})

(defn load-schema
  "Loads the GraphQL schema with resolvers attached."
  []
  (try
    (log/info "Attaching resolvers to schema...")
    (let [schema-with-resolvers (util/attach-resolvers schema-edn resolvers-map)
          final-schema (util/inject-scalar-transformers schema-with-resolvers scalar-transformers)]
      (log/info "Schema loaded successfully")
      final-schema)
    (catch Exception e
      (log/error e "Failed to load schema")
      (throw e))))
