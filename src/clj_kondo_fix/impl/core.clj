(ns clj-kondo-fix.impl.core
  (:require [clojure.java.io :as io]
            [clj-kondo.core :as kondo]
            [clj-kondo-fix.impl.rules :as rules]
            [clj-kondo-fix.impl.utils :refer [read-lines write-lines!]]))

(defn resolve-files [paths]
  (let [result (atom [])]
    (doseq [p paths]
      (let [f (io/file p)]
        (if (.isDirectory f)
          (doseq [file (file-seq f)
                  :when (and (.isFile file)
                             (re-find #"\.clj[cst]?$" (.getName file)))]
            (swap! result conj (.getAbsolutePath file)))
          (swap! result conj (.getAbsolutePath f)))))
    @result))

(defn normalize-finding [finding]
  ;; clj-kondo JVM API returns :filename and :row; normalize to :file and :line
  (-> finding
      (assoc :file (:filename finding))
      (assoc :line (:row finding))
      (dissoc :filename :row)))

(defn run-kondo [paths config]
  (let [files (resolve-files paths)
        result (kondo/run! {:lint files
                            :config (merge config {:linters {:namespace-name-mismatch {:level :off}}})})]
    (map normalize-finding (:findings result))))

(defn group-findings-by-file [findings]
  (reduce (fn [acc f]
            (update acc (:file f) (fnil conj []) f))
          {} findings))

(defn expand-findings-by-rule [findings active-rules]
  (reduce-kv (fn [acc rule-key rule-def]
               (let [matching (rules/findings-matching-rule findings rule-key rule-def)]
                 (if (seq matching)
                   (assoc acc rule-key {:rule-def rule-def
                                        :findings (group-findings-by-file matching)})
                   acc)))
             {} active-rules))

(defn apply-rules-to-file
  "Read the file once, apply each applicable rule in sequence threading
   the modified line vector through, return {:fixed N :changed? bool :lines [...]}."
  [file-path rule-findings log]
  (let [initial-lines (read-lines file-path)]
    (loop [[[_rule-key {:keys [rule-def findings]}] & more] (seq rule-findings)
           current-lines initial-lines
           total-fixed 0]
      (if (nil? rule-def)
        {:fixed total-fixed
         :lines current-lines
         :changed? (not= current-lines initial-lines)}
        (let [file-findings  (get findings file-path [])
              fix-fn         (:fix-fn rule-def)
              ;; Extract :fix-contexts from the rule's :config map and pass it
              ;; as the 5th arg.  Other fix functions ignore the extra arg via
              ;; their default arity.
              fix-contexts   (get-in rule-def [:config :fix-contexts])
              result         (if fix-contexts
                               (fix-fn file-path current-lines file-findings log fix-contexts)
                               (fix-fn file-path current-lines file-findings log))]
          (recur more (:lines result) (+ total-fixed (:fixed result))))))))

(defn fix!
  [{:keys [lint config dry-run rules]
    :or {dry-run false}}]
  (let [log (atom [])
        findings (run-kondo lint (or config {}))
        active-rules (rules/resolve-rules (when rules (map keyword rules)))
        rule-findings (expand-findings-by-rule findings active-rules)
        files (->> findings (map :file) distinct sort)
        results (mapv (fn [file-path]
                        (let [result (apply-rules-to-file file-path rule-findings log)]
                          (when (and (:changed? result) (not dry-run))
                            (write-lines! file-path (:lines result)))
                          {:file file-path
                           :fixed (:fixed result)
                           :changed? (:changed? result)}))
                      files)]
    {:results results
     :findings findings
     :summary {:files-scanned (count files)
               :files-changed (count (filter :changed? results))
               :total-fixed (reduce + 0 (map :fixed results))}
     :log @log}))
