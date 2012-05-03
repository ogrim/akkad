(ns akkad.tools
  (:require [clojure.string :as str]))

(defmacro redir
  [filename & body]
  `(with-open [w# (clojure.java.io/writer ~filename)]
     (binding [*out* w#] ~@body)))

(defn in? [element seq]
  (if (some #{element} seq) true false))

(defn trim-trailing-punctuation [s]
  (first (str/split s #"[.,:;!?]+\Z")))

(defn punctuation? [c]
  (in? c #{\. \, \: \; \! \? }))

(defn strip-newlines [s]
  (->> s (map #(if (= % \newline) " " %)) (apply str)))
