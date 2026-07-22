(ns clj-kondo-fix.impl.fixes.use
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn- has-only?
  [line use-col-idx]
  (let [after-use (subs line (+ use-col-idx 4))]
    (.contains after-use ":only")))

(defn- ns-after-use
  "Extract the namespace name from bare (:use <ns>) form."
  [line use-col-idx]
  (let [after-use (subs line (+ use-col-idx 4))]
    (some-> (re-find #"\s+([\w.\-+*!?']+)" after-use) second)))

(defn fix-use-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (>= col-idx (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [line (nth current-lines line-idx)]
                             (if (has-only? line col-idx)
                               ;; (:use [ns :only [syms]]) → (:require [ns :refer [syms]])
                               (let [use-end (+ col-idx 4)
                                     line2   (str (subs line 0 col-idx) ":require" (subs line use-end))
                                     only-idx (.indexOf line2 ":only")]
                                 (if (neg? only-idx)
                                   [current-lines nil]
                                   (let [line3 (str (subs line2 0 only-idx) ":refer" (subs line2 (+ only-idx 5)))]
                                     (swap! log conj (str "  " fu ":" (:line f) "  :use → :require, :only → :refer"))
                                     [(assoc current-lines line-idx line3) true])))
                               ;; bare (:use <ns>) → (:require [<ns> :refer :all])
                               (if-let [ns-name (ns-after-use line col-idx)]
                                 (let [use-end      (+ col-idx 4)
                                       rest-of-line (subs line use-end)
                                       ns-idx       (.indexOf rest-of-line ns-name)
                                       ns-end       (+ ns-idx (count ns-name))
                                       rest-after   (subs rest-of-line ns-end)
                                       replacement  (str ":require [" ns-name " :refer :all]")
                                       new-line     (str (subs line 0 col-idx) replacement rest-after)]
                                   (swap! log conj (str "  " fu ":" (:line f)
                                                        "  :use → :require [" ns-name " :refer :all]"))
                                   [(assoc current-lines line-idx new-line) true])
                                 [current-lines nil])))))))))
