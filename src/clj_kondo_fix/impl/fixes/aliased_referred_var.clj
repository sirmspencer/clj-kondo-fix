(ns clj-kondo-fix.impl.fixes.aliased-referred-var
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn- qualified-sym-end
  [line start-idx]
  (let [len (count line)]
    (loop [i start-idx]
      (if (or (>= i len)
              (Character/isWhitespace (get line i))
              (#{\( \) \[ \] \{ \} \" \, \; \@ \^ \~ \`} (get line i)))
        i
        (recur (inc i))))))

(defn- var-and-alias-from-msg
  [msg]
  (when-let [[_ var-name alias] (re-find #"Var (\S+) is referred but used via alias: (\S+)" msg)]
    {:var-name var-name :alias alias}))

(defn- skip-paren
  "If col-idx points to an opening paren, advance past it and any whitespace."
  [line col-idx]
  (let [ch (get line col-idx)]
    (if (#{\(} ch)
      (let [next-col (inc col-idx)]
        (loop [i next-col]
          (if (and (< i (count line)) (Character/isWhitespace (get line i)))
            (recur (inc i))
            i)))
      col-idx)))

(defn fix-aliased-referred-var-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx  (dec (:line f))
                             col-idx   (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (>= col-idx (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [line    (nth current-lines line-idx)
                                 sym-col (skip-paren line col-idx)
                                 end     (qualified-sym-end line sym-col)
                                 sym     (subs line sym-col end)
                                 parsed  (var-and-alias-from-msg (:message f))]
                             (if (or (nil? parsed) (<= end sym-col) (not (.contains sym "/")))
                               [current-lines nil]
                               (let [var-name (:var-name parsed)
                                     new-sym  var-name]
                                 (if (= new-sym sym)
                                   [current-lines nil]
                                   (let [new-line (str (subs line 0 sym-col)
                                                       new-sym
                                                       (subs line end))]
                                     (swap! log conj (str "  " fu ":" (:line f)
                                                          "  " sym " → " new-sym))
                                     [(assoc current-lines line-idx new-line) true])))))))))))
