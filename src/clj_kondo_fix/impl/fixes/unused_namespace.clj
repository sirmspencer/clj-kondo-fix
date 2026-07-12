(ns clj-kondo-fix.impl.fixes.unused-namespace
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.require-entry :refer [remove-require-finding
                                                      cleanup-empty-clauses]]))

(defn fix-unused-ns-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [ls f] (remove-require-finding ls f fu log))
                     cleanup-empty-clauses)))
