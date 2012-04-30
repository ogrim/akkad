(ns akkad.tools)

(defmacro redir
  [filename & body]
  `(with-open [w# (clojure.java.io/writer ~filename)]
     (binding [*out* w#] ~@body)))
