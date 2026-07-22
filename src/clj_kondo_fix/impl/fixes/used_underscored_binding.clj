(ns clj-kondo-fix.impl.fixes.used-underscored-binding
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(def clj-id-chars #"[a-zA-Z0-9_\-?!*+<>='.']")

(defn- replace-binding
  [line old-name new-name]
  (let [pat (re-pattern (str "(?<=^|[^a-zA-Z0-9_\\-?!*+<>='.])"
                             (java.util.regex.Pattern/quote old-name)
                             "(?=[^a-zA-Z0-9_\\-?!*+<>='.]|$)"))]
    (str/replace line pat new-name)))

(defn fix-used-underscored-binding-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [binding-name (some-> (re-find #"Used binding is marked as unused: (.+)" (:message f))
                                                   second)]
                         (if (and binding-name
                                  (.startsWith binding-name "_")
                                  (> (count binding-name) 1))
                           (let [new-name  (subs binding-name 1)
                                 changed?  (volatile! false)
                                 new-lines (mapv (fn [line]
                                                   (let [new-line (replace-binding line binding-name new-name)]
                                                     (when (not= line new-line)
                                                       (vreset! changed? true))
                                                     new-line))
                                                 current-lines)]
                             (if @changed?
                               (do (swap! log conj (str "  " fu ":" (:line f)
                                                        "  " binding-name " → " new-name))
                                   [new-lines true])
                               [current-lines nil]))
                           [current-lines nil]))))))
