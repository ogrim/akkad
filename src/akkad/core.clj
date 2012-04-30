(ns akkad.core
  (:require [net.cgrand.moustache :as m]
            [net.cgrand.enlive-html :as html]
            [akkad.database :as db])
  (:use [ring.adapter.jetty]
        [ring.middleware resource reload file params]
        [ring.util.response])
  (:gen-class :main true))

;(db/get-page "")

(def mw-di {:url "http://www.merriam-webster.com/dictionary/militate" :parser di-fn})
(def mw-th {:url "http://www.merriam-webster.com/thesaurus/imitate" :parser th-fn})
(def tlf1881 {:url "http://www.1881.no/?Query=97610194&qt=8" :parser tlf-fn})

(def routes
  (m/app
   (wrap-file "resources")
   ["" &] (delegate view-start-page)
   ["servicename" &] {:get (fn [_] (->> service-fn response))
                      :post (wrap-params service-fn)}))

(defn -main [port]
  (run-jetty #'routes {:port (Integer/parseInt port) :join? false}))

; (def server (-main "8087"))
