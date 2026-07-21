(ns clj-kondo-fix.impl.fixes.aliased-namespace-symbol
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn- qualified-sym-end
  "Scan forward from start-idx to find end of a qualified symbol."
  [line start-idx]
  (let [len (count line)]
    (loop [i start-idx]
      (if (or (>= i len)
              (Character/isWhitespace (get line i))
              (#{\( \) \[ \] \{ \} \" \, \; \@ \^ \~ \`} (get line i)))
        i
        (recur (inc i))))))

(defn- alias-from-msg
  "Extract the alias from the finding message.
  Message format: 'An alias is defined for <ns>: <alias>'"
  [msg]
  (some-> (re-find #"An alias is defined for .+: (.+)" msg) second))

(defn fix-aliased-namespace-symbol-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx  (dec (:line f))
                             col-idx   (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (>= col-idx (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [line  (nth current-lines line-idx)
                                 end   (qualified-sym-end line col-idx)
                                 sym   (subs line col-idx end)
                                 alias (alias-from-msg (:message f))]
                             (if (or (nil? alias) (<= end col-idx) (not (.contains sym "/")))
                               [current-lines nil]
                               (let [slash-idx (.indexOf sym "/")
                                     ns-part  (subs sym 0 slash-idx)
                                     name     (subs sym (inc slash-idx))
                                     new-sym  (str alias "/" name)]
                                 (if (= new-sym sym)
                                   [current-lines nil]
                                   (let [new-line (str (subs line 0 col-idx)
                                                       new-sym
                                                       (subs line end))]
                                     (swap! log conj (str "  " fu ":" (:line f)
                                                          "  " sym " → " new-sym))
                                     [(assoc current-lines line-idx new-line) true])))))))))))
