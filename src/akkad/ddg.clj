(ns akkad.ddg
  (:require [net.cgrand.enlive-html :as e]
            [clojure.string :as str]
            [clojure.data.json :as json])
  (:use [akkad.database :only [fetch-page]]
        [akkad.tools]))

(def bang-list "https://duckduckgo.com/bang.html")

(defn clean-str [s]
  (apply str (filter #(not (in? % [\space \: \)])) s)))

(defn name->keyword [s]
  (-> s clean-str (str/replace \( \-) str/lower-case keyword))

(defn parse-block [block]
  (let [name (-> (e/select block [:b]) first :content first)
        commands (->> block :content second str/split-lines (filter seq) (into []))]
    {(name->keyword name) commands}))

(defn get-bangs []
  (let [page (-> bang-list fetch-page :nodes)
        categories (->> (e/select page [:i]) (map #(->> % :content first name->keyword)))
        blocks (->> (e/select page [:div#content_internal :ul])
                    (drop 2) (map :content) (filter #(not= (first %) "\n")))]
    (into {} (map #(hash-map %2 (into {} (map parse-block %1))) blocks categories))))
