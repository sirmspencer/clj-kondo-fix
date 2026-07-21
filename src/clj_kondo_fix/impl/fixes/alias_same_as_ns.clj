(ns clj-kondo-fix.impl.fixes.alias-same-as-ns
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.require-entry :refer [cleanup-empty-clauses
                                                      remove-bare-requires]]
            [clj-kondo-fix.impl.utils :refer [find-opening-bracket
                                              remove-token-span
                                              word-end-pos]]))

(defn- extract-alias-name [msg]
  (second (re-find #"^Alias same as namespace name: (.+)$" msg)))

(defn- find-as-keyword-pos
  "Find the :as keyword on line that precedes the alias at col-idx.
   Returns the index of ':' or nil."
  [line col-idx]
  (let [pre (.substring ^String line 0 col-idx)
        idx (.lastIndexOf ^String pre ":as")]
    (when (and (>= idx 0)
               (re-find #"^\s+" (.substring ^String line (+ idx 3) col-idx))
               (or (zero? idx)
                   (re-find #"[\s\[]" (str (.charAt ^String line (dec idx))))))
      idx)))

(defn- ns-name-from-entry [lines line-idx as-idx]
  (when-let [{:keys [line col]} (find-opening-bracket lines line-idx as-idx)]
    (let [bracket-line (nth lines line)
          after-bracket (.substring ^String bracket-line (inc col))]
      (second (re-find #"^(\S+)" after-bracket)))))

(defn fix-alias-same-as-ns-in-file [file-path lines findings log]
  (let [fu       (->display-path file-path)
        ns-names (atom [])]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [alias-name (extract-alias-name (:message f))
                             line-idx   (dec (:line f))
                             col-idx    (dec (:col f))]
                         (if (or (nil? alias-name)
                                 (< line-idx 0)
                                 (>= line-idx (count current-lines)))
                           [current-lines nil]
                           (let [line   (nth current-lines line-idx)
                                 as-idx (find-as-keyword-pos line col-idx)]
                             (if (nil? as-idx)
                               [current-lines nil]
                               (let [alias-end (word-end-pos line col-idx)
                                     new-line  (remove-token-span line as-idx alias-end)
                                     new-lines (assoc current-lines line-idx new-line)
                                     ns-name   (ns-name-from-entry current-lines line-idx as-idx)]
                                 (when ns-name
                                   (swap! ns-names conj ns-name))
                                 (swap! log conj (str "  " fu ":" (:line f)
                                                      "  remove redundant alias: " alias-name))
                                 [new-lines true]))))))
                     (fn [current-lines]
                       (-> current-lines
                           (remove-bare-requires (distinct @ns-names) fu log)
                           cleanup-empty-clauses)))))
