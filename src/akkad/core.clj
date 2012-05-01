(ns akkad.core
  (:require [net.cgrand.moustache :as m]
            [net.cgrand.enlive-html :as html]
            [akkad.database :as db])
  (:use [ring.adapter.jetty]
        [ring.middleware resource reload file params]
        [ring.util.response]
        [ring.util.codec :only [url-encode]])
  (:gen-class :main true))

;; (def mw-di {:url "http://www.merriam-webster.com/dictionary/militate" :parser di-fn})
;; (def mw-th {:url "http://www.merriam-webster.com/thesaurus/imitate" :parser th-fn})

(defn select-blocks [url selector]
  (html/select (db/get-page url) selector))

(defn extract [nodes selector]
  (-> (html/select nodes selector) first :content first))

(defn tlf-process [s]
  (->> (re-seq #"[0-9]+" s)
       (apply str)
       (partition-all 2)
       (interpose " ")
       (map #(apply str %))
       (apply str)))

(def data-sources
  {:1881 {:url "http://www.1881.no/?query=aleksander&type=person"
          :query-fn #(str "http://www.1881.no/?query=" (url-encode %) "&type=person")
          :selector [:div#content_main :div.listing]
          :parser {:name {:selector [:h3 :a]}
                   :tlf {:selector [:h3 :span] :postprocess tlf-process}
                   :addr {:selector [:p.listing_address :span]}}}})

(defn parse-block [block parser]
  (->> (for [[key {:keys [selector postprocess]}] parser]
         (if (nil? postprocess)
           {key (extract block selector)}
           {key (-> (extract block selector) postprocess)}))
       (into {})))

(defn parse-page [url selector parser]
  (let [blocks (select-blocks url selector)]
    (->> (map #(hash-map %2 (parse-block %1 parser)) blocks (range 1 (inc (count blocks))))
         (into (sorted-map)))))

(defn query [source query]
  (let [{:keys [url query-fn selector parser]} (get data-sources source)]
    (parse-page (query-fn query) selector parser)))

; (query :1881 "Person name")

(def routes
  (m/app
   (wrap-file "resources")
   ["" &] (delegate view-start-page)
   ["servicename" &] {:get (fn [_] (->> service-fn response))
                      :post (wrap-params service-fn)}))

(defn -main [port]
  (run-jetty #'routes {:port (Integer/parseInt port) :join? false}))

; (def server (-main "8087"))
