(ns akkad.database
  (:use [korma core db])
  (:require [net.cgrand.enlive-html :as html]
            [clojure.java.jdbc :as sql]
            [clj-http.client :as http])
  (:import [java.io StringReader]))

(def dbspec
  {:classname   "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname     "akkad.db"})

(defdb db dbspec)

(def user-agent-fake "Opera/9.80 (Windows NT 6.1; WOW64; U; en) Presto/2.10.229 Version/11.62")
(def user-agent "akkad 0.0.1-SNAPSHOT (Clojure 1.3.0 CLI lookup tool)")

(defn create-tables []
  (do
    (sql/create-table "websites"
                    [:url :text "PRIMARY KEY"]
                    [:html :text]
                    [:status :int]
                    [:timestamp :datetime])
    (sql/do-commands "CREATE INDEX idx_websites ON websites(url)")))

(defn invoke-with-connection [f]
  (sql/with-connection dbspec (sql/transaction (f))))

(try (invoke-with-connection create-tables) (catch Exception _ nil))

(defn timestamp []
  (java.sql.Timestamp. (.getTime (java.util.Date.))))

(defentity websites
  (pk :url)
  (table :websites)
  (entity-fields :url :html :status :timestamp))

(defn page-exists? [url]
  (if (seq (select websites (where {:url url}))) true false))

(defn fetch-page [url]
  (let [resource (http/get url {:headers {"User-Agent" user-agent}
                                :throw-exceptions false})]
    {:nodes (-> resource :body StringReader. html/html-resource)
     :status (:status resource)}))

(defn persist-page [url]
  (let [{:keys [nodes status]} (fetch-page url)]
   (insert websites (values {:url url
                             :html (apply str nodes)
                             :status status
                             :timestamp (timestamp)}))))

(defn delete-page [url]
  (delete websites (where {:url url})))

(defn get-page [url]
  (let [[query] (select websites (where {:url url}))]
    (if (empty? query)
      (do (persist-page url) (get-page url))
      {:html (read-string (:html query))
       :status (:status query)})))
