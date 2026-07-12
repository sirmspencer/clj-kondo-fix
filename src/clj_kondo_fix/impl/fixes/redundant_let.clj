(ns clj-kondo-fix.impl.fixes.redundant-let
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn reindent-line
  "Strip old-leading spaces and prepend new-leading spaces.
   Returns line unchanged if it does not have exactly old-leading leading spaces."
  [line old-leading new-leading]
  (if (str/blank? line)
    line
    (let [actual (count (re-find #"^ *" line))]
      (if (= actual old-leading)
        (str (apply str (repeat (max 0 new-leading) " ")) (subs line old-leading))
        line))))

(defn find-outer-let
  "Scan backward from inner-line-idx to find the nearest enclosing (let.
   Returns {:line :col :close-line :close-col} or nil."
  [lines inner-line-idx inner-col-idx]
  (loop [i inner-line-idx]
    (when (>= i 0)
      (let [line      (nth lines i)
            max-col   (if (= i inner-line-idx) inner-col-idx (count line))
            portion   (subs line 0 max-col)
            candidates
            (loop [from 0 acc []]
              (let [idx (.indexOf portion "(let" from)]
                (if (neg? idx)
                  acc
                  (let [after (+ idx 4)
                        nch   (when (< after (count line)) (nth line after))]
                    (if (or (nil? nch) (= nch \space) (= nch \[))
                      (if-let [[ml mc] (find-matching-bracket-across-lines lines i idx)]
                        (if (or (> ml inner-line-idx)
                                (and (= ml inner-line-idx) (>= mc inner-col-idx)))
                          (recur (inc idx) (conj acc {:line i :col idx :close-line ml :close-col mc}))
                          (recur (inc idx) acc))
                        (recur (inc idx) acc))
                      (recur (inc idx) acc))))))]
        (if (seq candidates)
          (last candidates)
          (recur (dec i)))))))

(defn- find-bracket-open
  "Scan right from start-col to find the first [ on line. Returns col or nil."
  [line start-col]
  (loop [j start-col]
    (when (< j (count line))
      (if (= \[ (nth line j)) j (recur (inc j))))))

(defn- spaces [n] (apply str (repeat (max 0 n) " ")))

(defn merge-lets
  "Merge the outer let (described by outer map) with the inner let at
   (inner-line-idx, inner-col-idx). Returns the new line vector, or nil
   to signal that this case should be skipped."
  [lines inner-line-idx inner-col-idx outer]
  (let [{OL  :line  OC  :col
         OCL :close-line  OCC :close-col} outer
        outer-line    (nth lines OL)
        outer-bv-open (find-bracket-open outer-line (+ OC 4))
        [OBL OBC]     (when outer-bv-open
                        (find-matching-bracket-across-lines lines OL outer-bv-open))]
    (when OBL
      (let [IL            inner-line-idx
            IC            inner-col-idx
            inner-line    (nth lines IL)
            inner-bv-open (find-bracket-open inner-line (+ IC 4))
            [IBL IBC]     (when inner-bv-open
                            (find-matching-bracket-across-lines lines IL inner-bv-open))
            [ICL ICC]     (find-matching-bracket-across-lines lines IL IC)]
        (when (and IBL ICL)
          (let [outer-bind-col    (+ OC 6)
                multi-line-outer? (not= OBL OL)
                single-line?      (= OL IL)]

            (if single-line?
              ;; ---- Single-line: pure string surgery on one line ----
              (let [line        outer-line
                    outer-binds (str/trim (subs line (inc outer-bv-open) OBC))
                    inner-binds (str/trim (subs line (inc inner-bv-open) IBC))
                    body-text   (subs line (inc IBC) ICC)
                    after-outer (subs line (inc OCC))
                    merged      (str "(let [" outer-binds " " inner-binds "]" body-text ")" after-outer)]
                (assoc lines OL (str (subs line 0 OC) merged)))

              ;; ---- Multi-line merge ----
              (let [new-outer-line
                    (if multi-line-outer?
                      outer-line
                      (subs outer-line 0 OBC))
                    stripped-obl-line
                    (when multi-line-outer?
                      (str/trimr (subs (nth lines OBL) 0 OBC)))
                    outer-middle-lines
                    (when multi-line-outer?
                      (subvec lines (inc OL) OBL))

                    intermediate (subvec lines (inc (if multi-line-outer? OBL OL)) IL)
                    moved-lines  (mapv #(reindent-line % (+ OC 2) OC) intermediate)

                    inner-bind-lines
                    (if (= IBL IL)
                      [(str (spaces outer-bind-col)
                            (subs inner-line (inc inner-bv-open) IBC)
                            "]")]
                      (vec
                       (concat
                        [(str (spaces outer-bind-col)
                              (str/trimr (subs inner-line (inc inner-bv-open))))]
                        (for [i (range (inc IL) IBL)]
                          (reindent-line (nth lines i) (+ IC 6) outer-bind-col))
                        [(reindent-line (nth lines IBL) (+ IC 6) outer-bind-col)])))

                    body-close-lines
                    (if (= ICL IL)
                      (let [body-inline  (str/trim (subs inner-line (inc IBC) ICC))
                            after-outer  (subs inner-line (inc OCC))]
                        (if (str/blank? body-inline)
                          :no-body
                          [(str (spaces (+ OC 2)) body-inline ")" after-outer)]))
                      (let [body-lines  (mapv #(reindent-line (nth lines %) (+ IC 2) (+ OC 2))
                                              (range (inc IBL) ICL))
                            close-line  (nth lines ICL)
                            close-mod   (str (subs close-line 0 ICC)
                                             (subs close-line (inc ICC)))
                            close-rein  (reindent-line close-mod (+ IC 2) (+ OC 2))]
                        (conj body-lines close-rein)))

                    [inner-bind-lines body-close-lines]
                    (if (= body-close-lines :no-body)
                      (let [after-outer (subs inner-line (inc OCC))]
                        [(conj (vec (butlast inner-bind-lines))
                               (str (last inner-bind-lines) ")" after-outer))
                         []])
                      [inner-bind-lines body-close-lines])]

                (vec (concat
                      (take OL lines)
                      moved-lines
                      [new-outer-line]
                      (when multi-line-outer? outer-middle-lines)
                      (when multi-line-outer? [stripped-obl-line])
                      inner-bind-lines
                      body-close-lines
                      (drop (inc OCL) lines)))))))))))

(defn fix-redundant-let-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [IL    (dec (:line f))
                             IC    (dec (:col f))
                             outer (find-outer-let current-lines IL IC)]
                         (if (nil? outer)
                           (do (swap! log conj (str "  " fu ":" (:line f) "  skip: could not find outer let"))
                               [current-lines nil])
                           (let [new-lines (merge-lets current-lines IL IC outer)]
                             (if (nil? new-lines)
                               (do (swap! log conj (str "  " fu ":" (:line f) "  skip: unsupported let structure"))
                                   [current-lines nil])
                               (do (swap! log conj (str "  " fu ":" (:line f) "  merge redundant let"))
                                   [new-lines true])))))))))
