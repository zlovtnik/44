(ns church-api.db
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

;; Define database connection spec
(def db-spec
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "database.sqlite"})

;; Define table schemas
(def tables
  {:members
   [[:id :integer "PRIMARY KEY AUTOINCREMENT"]
    [:first_name :text "NOT NULL"]
    [:last_name :text "NOT NULL"]
    [:email :text "UNIQUE"]
    [:phone :text]
    [:address :text]
    [:join_date :text]
    [:status :text]
    [:created_at :datetime "DEFAULT CURRENT_TIMESTAMP"]
    [:updated_at :datetime "DEFAULT CURRENT_TIMESTAMP"]]
   
   :events
   [[:id :integer "PRIMARY KEY AUTOINCREMENT"]
    [:name :text "NOT NULL"]
    [:description :text]
    [:event_date :text "NOT NULL"]
    [:location :text]
    [:created_at :datetime "DEFAULT CURRENT_TIMESTAMP"]
    [:updated_at :datetime "DEFAULT CURRENT_TIMESTAMP"]]
   
   :groups
   [[:id :integer "PRIMARY KEY AUTOINCREMENT"]
    [:name :text "NOT NULL"]
    [:description :text]
    [:leader_id :integer "REFERENCES members(id)"]
    [:created_at :datetime "DEFAULT CURRENT_TIMESTAMP"]
    [:updated_at :datetime "DEFAULT CURRENT_TIMESTAMP"]]
   
   :attendances
   [[:id :integer "PRIMARY KEY AUTOINCREMENT"]
    [:member_id :integer "NOT NULL REFERENCES members(id)"]
    [:event_id :integer "NOT NULL REFERENCES events(id)"]
    [:attended_at :datetime "DEFAULT CURRENT_TIMESTAMP"]
    [:created_at :datetime "DEFAULT CURRENT_TIMESTAMP"]]
   
   :donations
   [[:id :integer "PRIMARY KEY AUTOINCREMENT"]
    [:member_id :integer "REFERENCES members(id)"]
    [:amount :real "NOT NULL"]
    [:donation_date :text "NOT NULL"]
    [:purpose :text]
    [:created_at :datetime "DEFAULT CURRENT_TIMESTAMP"]]
   
   :member_groups
   [[:member_id :integer "NOT NULL REFERENCES members(id)"]
    [:group_id :integer "NOT NULL REFERENCES groups(id)"]
    [:joined_at :datetime "DEFAULT CURRENT_TIMESTAMP"]
    ["PRIMARY KEY" "(member_id, group_id)"]]})

;; Create tables if they don't exist
(defn create-tables! []
  (doseq [[table-name table-spec] tables]
    (try
      (jdbc/db-do-commands db-spec
                          (jdbc/create-table-ddl table-name table-spec
                                                {:conditional? true}))
      (log/info "Created or verified table" table-name)
      (catch Exception e
        (log/error e "Failed to create table" table-name)))))

;; Initialize database
(defn init-db! []
  (log/info "Initializing database...")
  (create-tables!))

;; CRUD operations for members
(defn create-member! [member]
  (jdbc/insert! db-spec :members member))

(defn get-member [id]
  (first (jdbc/query db-spec ["SELECT * FROM members WHERE id = ?" id])))

(defn get-members []
  (jdbc/query db-spec ["SELECT * FROM members"]))

(defn update-member! [id member]
  (jdbc/update! db-spec :members member ["id = ?" id]))

(defn delete-member! [id]
  (jdbc/delete! db-spec :members ["id = ?" id]))

;; CRUD operations for events
(defn create-event! [event]
  (jdbc/insert! db-spec :events event))

(defn get-event [id]
  (first (jdbc/query db-spec ["SELECT * FROM events WHERE id = ?" id])))

(defn get-events []
  (jdbc/query db-spec ["SELECT * FROM events"]))

(defn update-event! [id event]
  (jdbc/update! db-spec :events event ["id = ?" id]))

(defn delete-event! [id]
  (jdbc/delete! db-spec :events ["id = ?" id]))

;; CRUD operations for groups
(defn create-group! [group]
  (jdbc/insert! db-spec :groups group))

(defn get-group [id]
  (first (jdbc/query db-spec ["SELECT * FROM groups WHERE id = ?" id])))

(defn get-groups []
  (jdbc/query db-spec ["SELECT * FROM groups"]))

(defn update-group! [id group]
  (jdbc/update! db-spec :groups group ["id = ?" id]))

(defn delete-group! [id]
  (jdbc/delete! db-spec :groups ["id = ?" id]))

;; CRUD operations for attendances
(defn create-attendance! [attendance]
  (jdbc/insert! db-spec :attendances attendance))

(defn get-attendance [id]
  (first (jdbc/query db-spec ["SELECT * FROM attendances WHERE id = ?" id])))

(defn get-attendances-by-event [event-id]
  (jdbc/query db-spec ["SELECT * FROM attendances WHERE event_id = ?" event-id]))

(defn get-attendances-by-member [member-id]
  (jdbc/query db-spec ["SELECT * FROM attendances WHERE member_id = ?" member-id]))

;; CRUD operations for donations
(defn create-donation! [donation]
  (jdbc/insert! db-spec :donations donation))

(defn get-donation [id]
  (first (jdbc/query db-spec ["SELECT * FROM donations WHERE id = ?" id])))

(defn get-donations-by-member [member-id]
  (jdbc/query db-spec ["SELECT * FROM donations WHERE member_id = ?" member-id]))

;; Member-Group relationships
(defn add-member-to-group! [member-id group-id]
  (jdbc/insert! db-spec :member_groups {:member_id member-id
                                       :group_id group-id}))

(defn remove-member-from-group! [member-id group-id]
  (jdbc/delete! db-spec :member_groups ["member_id = ? AND group_id = ?" member-id group-id]))

(defn get-group-members [group-id]
  (jdbc/query db-spec ["SELECT m.* FROM members m 
                       JOIN member_groups mg ON m.id = mg.member_id 
                       WHERE mg.group_id = ?" group-id]))

(defn get-member-groups [member-id]
  (jdbc/query db-spec ["SELECT g.* FROM groups g 
                       JOIN member_groups mg ON g.id = mg.group_id 
                       WHERE mg.member_id = ?" member-id]))

;; Member attendances
(defn get-member-attendances [member-id]
  (jdbc/query db-spec ["SELECT e.* FROM events e
                       JOIN attendances a ON e.id = a.event_id
                       WHERE a.member_id = ?" member-id]))

;; Event attendances
(defn get-event-attendances [event-id]
  (jdbc/query db-spec ["SELECT * FROM attendances
                       WHERE event_id = ?" event-id]))

;; Member donations
(defn get-member-donations [member-id]
  (jdbc/query db-spec ["SELECT * FROM donations
                       WHERE member_id = ?" member-id]))
