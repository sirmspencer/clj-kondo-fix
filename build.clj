(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'clj-kondo-fix/clj-kondo-fix)
(def version "0.1.0")
(def class-dir "target/classes")
(def uber-file (format "target/clj-kondo-fix-%s-standalone.jar" version))
(def native-file "target/clj-kondo-fix")

(defn- build-uber [{:keys [basis uber-file]}]
  (println "Cleaning target...")
  (b/delete {:path "target"})
  (println "Copying sources and resources...")
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (println "Compiling Clojure sources...")
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir
                  :bindings {#'clojure.core/*warn-on-reflection* true}
                  :java-opts ["-Dclojure.compiler.direct-linking=true"
                              "-Dclojure.spec.skip-macros=true"
                              "-Dborkdude.dynaload.aot=true"]})
  (println "Building uberjar...")
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'clj-kondo-fix.main})
  (println (str "Done: " uber-file)))

(defn uber [_]
  (build-uber {:basis (b/create-basis {:project "deps.edn"})
               :uber-file uber-file}))

(defn- find-native-image []
  (if-let [graal-home (System/getenv "GRAALVM_HOME")]
    (str graal-home "/bin/native-image")
    "native-image"))

(defn native [_]
  (build-uber {:basis (b/create-basis {:project "deps.edn" :aliases [:native]})
               :uber-file uber-file})
  (println "Building native image...")
  (let [{:keys [exit]} (b/process {:command-args [(find-native-image)
                                                  "-jar" uber-file
                                                  "-o" native-file
                                                  "--no-fallback"
                                                  "--features=clj_easy.graal_build_time.InitClojureClasses"
                                                  "-H:+ReportExceptionStackTraces"]})]
    (when-not (zero? exit)
      (throw (ex-info "native-image failed" {:exit exit})))
    (println (str "Done: " native-file))))
