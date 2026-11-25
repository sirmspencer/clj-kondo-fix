(ns clj-kondo-fix.core
  "Public API for clj-kondo-fix.

   Usage:
     (require '[clj-kondo-fix.core :as clj-kondo-fix])

     (clj-kondo-fix/fix! {:lint [\"src/\"]
                           :dry-run true})

     (clj-kondo-fix/print! result {:output :text})"
  (:require [clj-kondo-fix.impl.core :as impl]
            [clojure.java.io :as io]))

(defn fix!
  "Run clj-kondo lint and apply auto-fixes.

   Options:
   - :lint    — seq of file/directory paths to lint and fix (required)
   - :config  — config map passed through to clj-kondo (optional)
   - :dry-run — when true, compute fixes but don't write files (default: false)
   - :rules   — seq of keyword rule names to apply (default: all known rules)

   Returns:
   {:results  [{:file \"...\" :fixed N :changed? bool} ...]
    :findings [...]   ; raw clj-kondo findings (normalized to :file/:line/:col)
    :summary  {:files-scanned N :files-changed N :total-fixed N}
    :log      [...]}"
  [opts]
  (impl/fix! opts))

(defn fix-source
  "Apply auto-fixes to source code given as a string.

   Options are the same as fix!, without :lint and :dry-run, plus:
   - :filename — name used to pick the language from its extension
                 (.clj, .cljs, or .cljc; default .clj)

   Returns {:source \"fixed source\" :result fix!-result}. :source is the input
   unchanged when nothing was fixed."
  [source {:keys [filename] :as opts}]
  (let [ext  (or (re-find #"\.clj[cs]?$" (str filename)) ".clj")
        dir  (.toFile (java.nio.file.Files/createTempDirectory
                       "clj-kondo-fix" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file dir (str "source" ext))]
    (try
      (spit file source)
      (let [result (impl/fix! (-> opts
                                  (dissoc :filename)
                                  (assoc :lint [(str file)] :dry-run false)))]
        {:source (if (pos? (-> result :summary :files-changed))
                   (slurp file)
                   source)
         :result result})
      (finally
        (io/delete-file file true)
        (io/delete-file dir true)))))

(defn print!
  "Print a fix! result map to *out*.

   Options:
   - :output — :text (default) or :edn"
  ([result] (print! result {:output :text}))
  ([result {:keys [output] :or {output :text}}]
   (case output
     :edn (println (pr-str result))
     (do
       (println)
       (doseq [msg (:log result)]
         (println msg))
       (println)
       (println "--- Summary ---")
       (println "Files scanned:" (-> result :summary :files-scanned))
       (println "Files changed:" (-> result :summary :files-changed))
       (println "Total fixed:" (-> result :summary :total-fixed))
       (println)))))
