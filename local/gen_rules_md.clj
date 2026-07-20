#!/usr/bin/env bb
;; Generates rules.md — a showcase of implemented rules plus reference tables
;; for not-yet-implemented and not-applicable rules.
;;
;; Sources:
;;   ~/ext-github/clj-kondo/doc/linters.md  — canonical rule list + descriptions
;;   src/clj_kondo_fix/impl/rules.clj        — rule-metadata (status/reason) + rule-definitions
;;   test/clj_kondo_fix/rules/<rule>/       — fixture files with ;;-; ... ;-;; tags
;;
;; Output:
;;   rules.md  (project root)
;;
;; Run from project root:
;;   clojure -M:gen-rules

(ns gen-rules-md
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

;; ---------------------------------------------------------------------------
;; Parse linters.md
;; ---------------------------------------------------------------------------

(defn parse-linters-md [path]
  (let [lines (str/split-lines (slurp path))]
    (loop [[line & rest] lines
           current-heading nil
           rules           []]
      (if (nil? line)
        rules
        (let [heading-m (re-find #"^### (.+)" line)
              kw-m      (or (re-find #"^\*Keyword:\*\s*`:([\w-]+)`" line)
                            (re-find #"^\*Keyword\*:\s*`:([\w-]+)`" line))]
          (cond
            heading-m
            (recur rest (second heading-m) rules)

            kw-m
            (let [kw   (second kw-m)
                  desc (loop [[l & ls] rest]
                          (cond
                            (nil? l)                                         nil
                            (str/blank? l)                                   (recur ls)
                            (re-find #"^\*Description:\*\s*(.+)" l)
                            (-> (re-find #"^\*Description:\*\s*(.+)" l)
                                second
                                (str/replace #"\.$" "")
                                str/trim)
                            (str/starts-with? l "*")                         nil
                            :else                                            (recur ls)))]
              (recur rest current-heading
                     (conj rules {:keyword kw
                                  :heading (or current-heading kw)
                                  :desc    (or desc "")})))

            :else
            (recur rest current-heading rules)))))))

;; ---------------------------------------------------------------------------
;; Parse rules.clj — extract rule-metadata (pure EDN block) + rule-definitions keys
;; ---------------------------------------------------------------------------

(defn parse-rule-metadata [path]
  (let [content (slurp path)]
    (when-let [m (re-find #"\(def rule-metadata\n([\s\S]*?)\)\n\n\(defn stub-fix-fn" content)]
      (edn/read-string (second m)))))

(defn parse-implemented-keys [path]
  (let [content (slurp path)]
    (->> (re-seq #"(?m)^\s+\{?:([\w?-]+)\n\s+\{?:message-re" content)
         (map second)
         set)))

;; ---------------------------------------------------------------------------
;; Fixture helpers
;; ---------------------------------------------------------------------------

(defn fixture-dir [rule-kw]
  (io/file "test/clj_kondo_fix/rules" (str/replace (name rule-kw) #"-" "_")))

(defn read-fixture [rule-kw slug suffix]
  "Read fixture file content, stripping leading ;;-; ... ;-;; tag if present."
  (let [f (io/file (fixture-dir rule-kw) (str slug suffix ".clj"))]
    (when (.exists f)
      (let [content (slurp f)]
        (str/replace content #"\A;;-;.*?;-;;\n" "")))))

(defn fixture-tag [rule-kw slug]
  "Extract the ;;-; ... ;-;; description from a fixture file, or nil if absent."
  (let [f (io/file (fixture-dir rule-kw) (str slug "-in.clj"))]
    (when (.exists f)
      (let [content (slurp f)
            m       (re-find #";;-;\s*(.+?)\s*;-;;" content)]
        (when m (second m))))))

(defn choose-examples [rule-kw _notes]
  (let [dir (fixture-dir rule-kw)]
    (when (.isDirectory dir)
      (let [pairs (->> (.listFiles dir)
                       (filter #(str/ends-with? (.getName %) "-in.clj"))
                       (keep (fn [f]
                               (let [slug (str/replace (.getName f) #"-in\.clj$" "")
                                     out  (io/file dir (str slug "-out.clj"))]
                                 (when (.exists out) slug)))))]
        (when (seq pairs) (vec pairs))))))

;; ---------------------------------------------------------------------------
;; Render sections
;; ---------------------------------------------------------------------------

(defn render-example [rule-kw slug]
  (let [in-raw  (read-fixture rule-kw slug "-in")
        out-raw (read-fixture rule-kw slug "-out")
        desc    (fixture-tag rule-kw slug)]
    (when (and in-raw out-raw)
      (str/join "\n"
                (remove nil?
                        [(when desc (str "**" desc "**\n"))
                         "```clojure"
                         (str/trimr in-raw)
                         "```"
                         ""
                         "↓"
                         ""
                         "```clojure"
                         (str/trimr out-raw)
                         "```"])))))

(defn render-implemented [rule-kw heading desc]
  (let [examples (choose-examples rule-kw nil)
        ex-blocks (keep #(render-example rule-kw %) examples)]
    (str/join "\n\n"
              (remove nil?
                      [(str "### :" (name rule-kw))
                       (str "**" heading "**")
                       (when (seq desc) desc)
                       (when (seq ex-blocks)
                         (str/join "\n\n---\n\n" ex-blocks))]))))

;; ---------------------------------------------------------------------------
;; README.md ## Rules section
;; ---------------------------------------------------------------------------

(defn rules-anchor [rule-kw]
  (str/replace (name rule-kw) #"[^a-z0-9-]" ""))

(defn readme-status-icon [s]
  (case s
    :implemented " ✅"
    :not-applicable " ❌"
    :skipped " ⚠️"
    ""))

(defn render-readme-rules [enriched]
  (str "## Rules\n\n"
       "✅ implemented · ❌ not applicable · ⚠️ skipped · (no icon) not yet implemented\n\n"
       "See [rules.md](rules.md) for implementation notes and before/after examples.\n\n"
       (str/join "\n"
                 (map (fn [r]
                        (let [anchor (rules-anchor (:keyword r))]
                          (str "- [:" (:keyword r) "](rules.md#" anchor ")"
                               (readme-status-icon (:status r)))))
                      enriched))))

;; ---------------------------------------------------------------------------
;; Main
;; ---------------------------------------------------------------------------

(defn -main [& _]
  (let [linters-path  (str (System/getProperty "user.home") "/ext-github/clj-kondo/doc/linters.md")
        rules-clj     "src/clj_kondo_fix/impl/rules.clj"
        out-path      "rules.md"

        all-rules     (parse-linters-md linters-path)
        metadata      (parse-rule-metadata rules-clj)
        impl-keys     (parse-implemented-keys rules-clj)

        enriched      (->> all-rules
                           (map (fn [r]
                                  (let [kw   (clojure.lang.Keyword/intern (:keyword r))
                                        meta (get metadata kw)]
                                    (assoc r :kw kw
                                           :status (or (:status meta)
                                                       (when (contains? impl-keys (:keyword r))
                                                         :implemented))
                                           :reason (:reason meta)))))
                           (sort-by :keyword))

        impl     (filter #(= :implemented (:status %)) enriched)
        skipped  (filter #(= :skipped     (:status %)) enriched)
        todo     (filter #(= :not-implemented (:status %)) enriched)
        na       (filter #(= :not-applicable  (:status %)) enriched)
        unknown  (filter #(nil? (:status %)) enriched)

        status-icon (fn [s]
                      (case s
                        :implemented    " ✅"
                        :not-applicable " ❌"
                        :not-implemented " ☹️"
                        :skipped        " ⚠️"
                        " 🚨"))

        ;; ---- Implemented section ----
        impl-section
        (str "## Implemented Rules\n\n"
             (str/join "\n\n---\n\n"
                       (map #(render-implemented (:kw %) (:heading %) (:desc %))
                            impl)))

        ;; ---- Not-yet-implemented table ----
        todo-table
        (str "## Not Yet Implemented\n\n"
             "These rules could potentially be auto-fixed but have not been tackled yet.\n\n"
             "| Rule | Description |\n"
             "| --- | --- |\n"
             (str/join "\n"
                       (map #(str "| `:" (:keyword %) "` | " (:desc %) " |")
                            todo)))

        ;; ---- Not-applicable table (includes skipped) ----
        na-rows  (concat skipped na)
        na-table
        (str "## Not Applicable\n\n"
             "These rules cannot be meaningfully auto-fixed.\n\n"
             "| Rule | Why |\n"
             "| --- | --- |\n"
             (str/join "\n"
                       (map #(str "| `:" (:keyword %) "` | "
                                  (or (:reason %) "") " |")
                            (sort-by :keyword na-rows))))

        ;; ---- Index ----
        index
        (str "## Index\n\n"
             (str/join "\n"
                       (map (fn [r]
                              (str "- [:" (:keyword r) "](#"
                                   (str/replace (:keyword r) #"[^a-z0-9-]" "")
                                   ")" (status-icon (:status r))))
                            enriched)))

        output
        (str/join "\n\n"
                  ["# clj-kondo-fix Rule Index"
                   (str (count impl) " implemented · "
                        (count todo) " not yet implemented · "
                        (count na)   " not applicable · "
                        (count skipped) " skipped")
                   index
                   impl-section
                   todo-table
                   na-table])]

    (spit out-path output)
    (println (str "Wrote " out-path))

    ;; Update README.md ## Rules section
    (let [readme-path "README.md"
          readme     (slurp readme-path)
          rules-s    (render-readme-rules enriched)
          new-readme (str/replace readme #"(?sm)^## Rules\n\n.*?(?=\n## |\z)" rules-s)]
      (spit readme-path new-readme)
      (println (str "Updated " readme-path)))

    (println (str "  " (count impl) " implemented  |  "
                  (count todo) " not yet implemented  |  "
                  (count na)   " not applicable"))))
