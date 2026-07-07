(ns clj-kondo-fix.main
  "CLI entry point for clj-kondo-fix.
   Mirrors the architecture of clj-kondo.main.

   Usage:
     clj-kondo-fix --lint src/
     clj-kondo-fix --lint src/ --fix
     clj-kondo-fix --lint src/ --fix --rules unused-namespace,unused-binding
     clj-kondo-fix --lint src/ --output edn
     clj-kondo-fix --version
     clj-kondo-fix --help"
  (:gen-class)
  (:require [clojure.string :as str]
            [clojure.tools.cli :refer [parse-opts]]
            [clj-kondo-fix.core :as clj-kondo-fix]))

(def cli-options
  [["-l" "--lint PATH" "File or directory to lint and fix (repeatable)"
    :default []
    :assoc-fn (fn [m k v] (update m k (fnil conj []) v))]
   ["-f" "--fix" "Apply fixes (default: dry-run)"
    :default false]
   ["-r" "--rules RULES" "Comma-separated rule names (default: all)"
    :default nil]
   ["-c" "--config EDN" "Config EDN string passed to clj-kondo"
    :default nil]
   ["-o" "--output FORMAT" "Output format: text or edn (default: text)"
    :default "text"
    :validate [#(contains? #{"text" "edn"} %) "Must be 'text' or 'edn'"]]
   ["-h" "--help" "Print usage"]
   ["-v" "--version" "Print version"]])

(def version "0.1.0")

(defn usage [summary]
  (str/join \newline
            ["Usage: clj-kondo-fix [options]"
             ""
             "Run clj-kondo lint and auto-fix common Clojure issues."
             ""
             "Options:"
             summary
             ""
             "Examples:"
             "  clj-kondo-fix --lint src/"
             "  clj-kondo-fix --lint src/ --fix"
             "  clj-kondo-fix --lint src/ --fix --rules unused-namespace,unused-binding"
             "  clj-kondo-fix --lint src/ --output edn"
             ""
             "When called without --fix, runs in dry-run mode (no files changed)."]))

(defn parse-args [args]
  (let [{:keys [options errors summary]} (parse-opts args cli-options)]
    (cond
      errors (do (doseq [e errors] (println e))
                 (System/exit 1))
      (:help options) (do (println (usage summary))
                          (System/exit 0))
      (:version options) (do (println "clj-kondo-fix" version)
                             (System/exit 0))
      (empty? (:lint options)) (do (println "Error: --lint is required")
                                   (println (usage summary))
                                   (System/exit 1))
      :else options)))

(defn -main [& args]
  (let [opts (parse-args args)
        output-format (keyword (:output opts))
        rules (when (:rules opts)
                (str/split (:rules opts) #","))
        config (when (:config opts)
                 (read-string (:config opts)))
        result (clj-kondo-fix/fix! {:lint (:lint opts)
                                    :config config
                                    :dry-run (not (:fix opts))
                                    :rules rules})]
    (clj-kondo-fix/print! result {:output output-format})
    (System/exit (if (pos? (-> result :summary :total-fixed)) 1 0))))
