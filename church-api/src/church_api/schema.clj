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
   {:groups (fn [member _ _]
              (map map-db-group (db/get-member-groups (:id member))))
    :attendances (fn [member _ _]
                   (map map-db-attendance (db/get-member-attendances (:id member))))
    :donations (fn [member _ _]
                 (map map-db-donation (db/get-member-donations (:id member))))}
   :Group
   {:leader (fn [group _ _]
              (map-db-member (db/get-member (:leaderId group))))
    :members (fn [group _ _]
               (map map-db-member (db/get-group-members (:id group))))}
   :Event
   {:attendances (fn [event _ _]
                   (map map-db-attendance (db/get-event-attendances (:id event))))}})

(def query-resolvers
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
             (map map-db-group (db/get-groups)))})

(def mutation-resolvers
  {:createMember (fn [_ args _]
                   (let [member-data (-> args :input)
                         result (db/create-member! (-> member-data
                                                       (set/rename-keys {:firstName :first_name
                                                                         :lastName :last_name
                                                                         :joinDate :join_date})))]
                     (map-db-member (assoc member-data :id (get-in result [0 :id])))))
   :updateMember (fn [_ args _]
                   (let [id (:id args)
                         member-data (-> args :input)
                         updated-data (-> member-data
                                          (set/rename-keys {:firstName :first_name
                                                            :lastName :last_name
                                                            :joinDate :join_date}))]
                     (db/update-member! id updated-data)
                     (map-db-member (assoc updated-data :id id))))
   :createAttendance (fn [_ args _]
                       (let [attendance-data (-> args :input)
                             result (db/create-attendance! attendance-data)]
                         (map-db-attendance (assoc attendance-data :id (get-in result [0 :id])))))
   :createDonation (fn [_ args _]
                     (let [donation-data (-> args :input)
                           result (db/create-donation! (-> donation-data
                                                           (set/rename-keys {:donationDate :donation_date})))]
                       (map-db-donation (assoc donation-data :id (get-in result [0 :id])))))
   :addMemberToGroup (fn [_ args _]
                       (let [{:keys [memberId groupId]} args]
                         (db/add-member-to-group! memberId groupId)
                         (map-db-group (db/get-group groupId))))
   :removeMemberFromGroup (fn [_ args _]
                            (let [{:keys [memberId groupId]} args]
                              (db/remove-member-from-group! memberId groupId)
                              (map-db-group (db/get-group groupId))))
   :deleteMember (fn [_ args _]
                   (let [id (:id args)]
                     (db/delete-member! id)
                     {:id id :success true}))
   :createEvent (fn [_ args _]
                  (let [event-data (-> args :input)
                        result (db/create-event! (-> event-data
                                                     (set/rename-keys {:eventDate :event_date})))]
                    (map-db-event (assoc event-data :id (get-in result [0 :id])))))
   :updateEvent (fn [_ args _]
                  (let [id (:id args)
                        event-data (-> args :input)
                        updated-data (-> event-data
                                         (set/rename-keys {:eventDate :event_date}))]
                    (db/update-event! id updated-data)
                    (map-db-event (db/get-event id))))
   :deleteEvent (fn [_ args _]
                  (let [id (:id args)]
                    (db/delete-event! id)
                    {:id id :success true}))
   :createGroup (fn [_ args _]
                  (let [group-data (-> args :input)
                        result (db/create-group! (-> group-data
                                                     (set/rename-keys {:leaderId :leader_id})))]
                    (map-db-group (assoc group-data :id (get-in result [0 :id])))))})

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
     :resolve :groups}},
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

;; Combine all resolvers into a single map
(def resolvers-map
  (merge field-resolvers
         {:Query query-resolvers
          :Mutation mutation-resolvers}))

(defn load-schema
  "Loads the GraphQL schema with resolvers attached."
  []
  (try
    (log/info "Attaching resolvers to schema...")
    (log/debug "Resolvers map:" resolvers-map)
    (let [schema-with-resolvers (util/attach-resolvers schema-edn resolvers-map)
          _ (log/debug "Schema with resolvers:" schema-with-resolvers)
          final-schema (util/inject-scalar-transformers schema-with-resolvers scalar-transformers)]
      (log/info "Schema loaded successfully")
      final-schema)
    (catch Exception e
      (log/error e "Failed to load schema")
      (throw e))))
     {:id {:type 'Int}
      :memberId {:type 'Int}
      :eventId {:type 'Int}
      :attendedAt {:type 'String}
      :createdAt {:type 'String}}

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
     :fields {:id {:type 'Int}
              :success {:type 'Boolean}}}

   :input-objects
   {:MemberInput
    {:fields
     {:firstName {:type '(non-null String)}
      :lastName {:type '(non-null String)}
      :email {:type 'String}
      :phone {:type 'String}
      :address {:type 'String}
      :birthDate {:type 'String}
      :joinedDate {:type 'String}}}

    :EventInput
    {:fields
     {:name {:type '(non-null String)}
      :description {:type 'String}
      :startDate {:type '(non-null String)}
      :endDate {:type 'String}
      :location {:type 'String}}}

    :GroupInput
    {:fields
     {:name {:type '(non-null String)}
      :description {:type 'String}}}

    :AttendanceInput
    {:fields
     {:memberId {:type 'Int}
      :eventId {:type 'Int}
      :attendedAt {:type 'String}}}

    :DonationInput
    {:fields
     {:memberId {:type 'Int}
      :amount {:type '(non-null Float)}
      :donationDate {:type '(non-null String)}
      :purpose {:type 'String}}}

    :MemberGroupInput
    {:fields
     {:memberId {:type 'Int}
      :groupId {:type 'Int}}}}

   :queries
   {:memberById
    {:type :Member
     :args {:id {:type 'Int}}
     :resolve :query/member-by-id}

    :allMembers
    {:type '(list :Member)
     :resolve :query/all-members}

    :eventById
    {:type :Event
     :args {:id {:type 'Int}}
     :resolve :query/event-by-id}

    :allEvents
    {:type '(list :Event)
     :resolve :query/all-events}

    :groupById
    {:type :Group
     :args {:id {:type 'Int}}
     :resolve :query/group-by-id}

    :allGroups
    {:type '(list :Group)
     :resolve :query/all-groups}

    :attendanceById
    {:type :Attendance
     :args {:id {:type 'Int}}
     :resolve :query/attendance-by-id}

    :attendancesByEvent
    {:type '(list :Attendance)
     :args {:eventId {:type 'Int}}
     :resolve :query/attendances-by-event}

    :attendancesByMember
    {:type '(list :Attendance)
     :args {:memberId {:type 'Int}}
     :resolve :query/attendances-by-member}

    :donationById
    {:type :Donation
     :args {:id {:type 'Int}}
     :resolve :query/donation-by-id}

    :donationsByMember
    {:type '(list :Donation)
     :args {:memberId {:type 'Int}}
     :resolve :query/donations-by-member}

    :groupMembers
    {:type '(list :Member)
     :args {:groupId {:type 'Int}}
     :resolve :query/group-members}

    :memberGroups
    {:type '(list :Group)
     :args {:memberId {:type 'Int}}
     :resolve :query/member-groups}}

   :mutations
   {:createMember
    {:type :Member
     :args {:input {:type :MemberInput}}
     :resolve :mutation/create-member}

    :updateMember
    {:type :Member
     :args {:id {:type 'Int}
            :input {:type :MemberInput}}
     :resolve :mutation/update-member}

    :deleteMember
    {:type :DeleteResult
     :args {:id {:type 'Int}}
     :resolve :mutation/delete-member}

    :createEvent
    {:type :Event
     :args {:input {:type :EventInput}}
     :resolve :mutation/create-event}

    :updateEvent
    {:type :Event
     :args {:id {:type 'Int}
            :input {:type :EventInput}}
     :resolve :mutation/update-event}

    :deleteEvent
    {:type :DeleteResult
     :args {:id {:type 'Int}}
     :resolve :mutation/delete-event}

    :createGroup
    {:type :Group
     :args {:input {:type :GroupInput}}
     :resolve :mutation/create-group}

    :updateGroup
    {:type :Group
     :args {:id {:type 'Int}
            :input {:type :GroupInput}}
     :resolve :mutation/update-group}

    :deleteGroup
    {:type :DeleteResult
     :args {:id {:type 'Int}}
     :resolve :mutation/delete-group}

    :createAttendance
    {:type :Attendance
     :args {:input {:type :AttendanceInput}}
     :resolve :mutation/create-attendance}

    :updateAttendance
    {:type :Attendance
     :args {:id {:type 'Int}
            :input {:type :AttendanceInput}}
     :resolve :mutation/update-attendance}

    :deleteAttendance
    {:type :DeleteResult
     :args {:id {:type 'Int}}
     :resolve :mutation/delete-attendance}

    :createDonation
    {:type :Donation
     :args {:input {:type :DonationInput}}
     :resolve :mutation/create-donation}

    :updateDonation
    {:type :Donation
     :args {:id {:type 'Int}
            :input {:type :DonationInput}}
     :resolve :mutation/update-donation}

    :deleteDonation
    {:type :DeleteResult
     :args {:id {:type 'Int}}
     :resolve :mutation/delete-donation}

    :addMemberToGroup
    {:type :MemberGroupResult
     :args {:input {:type :MemberGroupInput}}
     :resolve :mutation/add-member-to-group}

    :removeMemberFromGroup
    {:type :MemberGroupResult
     :args {:input {:type :MemberGroupInput}}
     :resolve :mutation/remove-member-from-group}}
