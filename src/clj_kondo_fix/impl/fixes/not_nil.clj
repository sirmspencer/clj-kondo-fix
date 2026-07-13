(ns clj-kondo-fix.impl.fixes.not-nil
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- open-paren-left
  [line col-idx]
  (loop [i (dec col-idx) depth 0]
    (if (< i 0)
      nil
      (let [ch (nth line i)]
        (cond
          (#{\) \] \}} ch) (recur (dec i) (inc depth))
          (#{\( \[ \{} ch) (if (zero? depth) i (recur (dec i) (dec depth)))
          :else (recur (dec i) depth))))))

(defn fix-not-nil-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx      (dec (:line f))
              col-idx       (dec (:col f))
              line          (nth current-lines line-idx)
              msg           (:message f)
              parent-open   (open-paren-left line col-idx)]
          (if (nil? parent-open)
            [current-lines nil]
            (let [parent-close-pos (find-matching-bracket-across-lines
                                     current-lines line-idx parent-open)]
              (if (nil? parent-close-pos)
                [current-lines nil]
                (let [[parent-close-line parent-close-col] parent-close-pos]
                  (if (not= parent-close-line line-idx)
                    [current-lines nil]
                    (let [nil-close-pos (find-matching-bracket-across-lines
                                          current-lines line-idx col-idx)]
                      (if (nil? nil-close-pos)
                        [current-lines nil]
                        (let [[nil-close-line nil-close-col] nil-close-pos]
                          (if (not= nil-close-line line-idx)
                            [current-lines nil]
                            (let [nil-inner (subs line (inc col-idx) nil-close-col)
                                  x-arg     (second (re-find #"^nil\?\s+(.*)$" nil-inner))]
                              (if (nil? x-arg)
                                [current-lines nil]
                                (let [prefix (subs line 0 parent-open)
                                      suffix (subs line (inc parent-close-col))
                                      new-line
                                      (cond
                                        (.contains msg "if-not (nil?")
                                        (let [rest (str/trim (subs line (inc nil-close-col) parent-close-col))]
                                          (str prefix "(if (some? " x-arg ") " rest ")" suffix))

                                        (.contains msg "when-not (nil?")
                                        (let [rest (str/trim (subs line (inc nil-close-col) parent-close-col))]
                                          (str prefix "(when (some? " x-arg ") " rest ")" suffix))

                                        (.contains msg "not (nil?")
                                        (str prefix "(some? " x-arg ")" suffix)

                                        :else nil)]
                                  (if (nil? new-line)
                                    [current-lines nil]
                                    (do
                                      (swap! log conj (str "  " fu ":" (:line f)
                                                           "  not-nil? → some?"))
                                      [(assoc current-lines line-idx new-line) true])))))))))))))))))))
