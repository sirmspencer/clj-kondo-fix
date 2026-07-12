(ns clj-kondo-fix.impl.driver
  (:require [clojure.string :as str]))

(defn ->display-path
  "Rewrite an absolute path to ~/... for use in log messages."
  [file-path]
  (str/replace file-path (str (System/getProperty "user.home")) "~"))

(defn reduce-findings
  "Drive a per-finding transform over a sorted, deduplicated set of findings.

   per-finding-fn : [current-lines finding] → [new-lines changed?]
   post-fn        : [lines] → lines  (optional; applied once after all findings)

   Returns {:fixed N :lines [...] :changed? bool}"
  ([lines findings per-finding-fn]
   (reduce-findings lines findings per-finding-fn nil))
  ([lines findings per-finding-fn post-fn]
   (let [sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
     (loop [[f & more] sorted
            current-lines lines
            fixed 0]
       (if (nil? f)
         (let [final (if post-fn (post-fn current-lines) current-lines)]
           {:fixed fixed :lines final :changed? (or (pos? fixed) (not= final lines))})
         (let [[new-lines changed?] (per-finding-fn current-lines f)]
           (recur more new-lines (if changed? (inc fixed) fixed))))))))
