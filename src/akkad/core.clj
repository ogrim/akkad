(ns akkad.core
  (:require [net.cgrand.moustache :as m]
            [net.cgrand.enlive-html :as e]
            [akkad.database :as db])
  (:use [akkad.tools]
        [ring.adapter.jetty]
        [ring.middleware resource reload file params]
        [ring.util.response]
        [ring.util.codec :only [url-encode]])
  (:import [de.l3s.boilerpipe.extractors KeepEverythingExtractor])
  (:gen-class :main true))

(defn extract-content [s]
  (.. KeepEverythingExtractor INSTANCE (getText s)))

(defn tlf-process [s]
  (->> (re-seq #"[0-9]+" s)
       (apply str)
       (partition-all 2)
       (interpose " ")
       (map #(apply str %))
       (apply str)))

(def data-sources
  {:1881 {:name "1881.no"
          :query-fn #(str "http://www.1881.no/?query=" (url-encode %) "&type=person")
          :200 {:selector [:div#content_main :div.listing]
                :parser {:name {:selector [:h3 :a]}
                         :tlf {:selector [:h3 :span] :postprocess tlf-process}
                         :addr {:selector [:p.listing_address :span]}}}}
   :mw-di {:name "Merriam-Webster Dictionary"
           :query-fn #(str "http://www.merriam-webster.com/dictionary/" (url-encode %))
           :200 {:selector [:div.KonaBody [:div.sblk]]
                 :parser {:headline {:selector [:span.ssens]
                                     :postprocess #(->> % e/emit* (apply str) extract-content strip-newlines .trim)}
                          }}
           :404 {:selector [:div.spelling-help :ol :li :a]
                 :parser {:alternatives {:postprocess #(-> % :content first)}}
                 }}
   :mw-th {:name "Merriam-Webster Thesaurus"
           :query-fn #(str "http://www.merriam-webster.com/thesaurus/" (url-encode %))
;           :200 {}
           :404 {:selector [:div.spelling-help :ol :li :a]
                 :parser {:alternatives {:postprocess #(-> % :content first)}}}}})

(defn extract [nodes selector]
  (-> (e/select nodes selector) first :content first))

(defn parse-block [block parser]
  (->> (for [[key {:keys [selector postprocess]}] parser]
         (cond (nil? postprocess) {key (extract block selector)}
               (nil? selector) {key (postprocess block)}
               :else {key (-> (e/select block selector) postprocess)}))
       (into {})))

(defn enumerate-blocks [blocks]
  (->> (map #(hash-map %2 %1) blocks (->> blocks count inc (range 1)))
       (into (sorted-map))))

(defn query [source-key query-string]
  (let [source (get data-sources source-key)
        query-url ((:query-fn source) query-string)
        {:keys [html status]} (db/get-page query-url)
        {:keys [selector parser] :as handlers} (get source (keyword (str status)))]
    (cond (nil? handlers) {:error (str "No handler for status " status " - URL was: " query-url)}
          :else {:blocks (->> (e/select html selector) (map #(parse-block % parser)) enumerate-blocks)})))

;; (def s1 (get data-sources :mw-di))
;; (def q1 ((:query-fn s1) "thesis"))
;; (def h1 (db/get-page q1))
;; (def html1 (:html h1))
;; (def status1 (:status h1))
;; (def handlers1 (get s1 :200))
;; (def sel1 (:selector handlers1))
;; (def par1 (:parser handlers1))

;; (def z1 (e/select html1 sel1))




; (query :1881 "Person name")
; (query :mw-di "Militate")
; (query :mw-th "Query")

(comment (def routes
   (m/app
    (wrap-file "resources")
    ["" &] (delegate view-start-page)
    ["servicename" &] {:get (fn [_] (->> service-fn response))
                       :post (wrap-params service-fn)})))

(comment (defn -main [port]
   (run-jetty #'routes {:port (Integer/parseInt port) :join? false})))

; (def server (-main "8087"))
