(ns clj-kondo-fix.impl.fixes.duplicate-field-name
  (:require [clj-kondo-fix.impl.utils :refer [word-end-pos remove-token-span]]))

(defn fix-duplicate-field-name-in-file [file-path lines findings log]
  (let [groups (->> findings
                    (group-by #(second (re-find #": (\S+)$" (:message %))))
                    (vals))
        to-remove (->> groups
                       (mapcat (fn [g]
                                 (when (> (count g) 1)
                                   (rest (sort-by (juxt :line :col) g)))))
                       (sort-by (juxt :line :col) #(compare %2 %1)))]
    (if (empty? to-remove)
      {:fixed 0 :lines lines :changed? false}
      (loop [[f & more] to-remove
             current-lines lines
             fixed 0]
        (if (nil? f)
          {:fixed fixed :lines current-lines :changed? (pos? fixed)}
          (let [line-idx (dec (:line f))
                col     (dec (:col f))
                line    (nth current-lines line-idx)
                end     (word-end-pos line col)
                new-line (remove-token-span line col end)]
            (recur more (assoc current-lines line-idx new-line) (inc fixed))))))))
