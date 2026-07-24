(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'clj-kondo-fix/clj-kondo-fix)
(def version "0.1.0")
(def class-dir "target/classes")
(def uber-file (format "target/clj-kondo-fix-%s-standalone.jar" version))

(def basis (b/create-basis {:project "deps.edn"}))

(defn uber [_]
  (println "Cleaning target...")
  (b/delete {:path "target"})
  (println "Copying sources and resources...")
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (println "Compiling Clojure sources...")
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir
                  :java-opts ["-Dclojure.compiler.direct-linking=true"
                              "-Dclojure.spec.skip-macros=true"]})
  (println "Building uberjar...")
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'clj-kondo-fix.main})
  (println (str "Done: " uber-file)))
