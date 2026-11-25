(ns build
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]))

(def lib 'clj-kondo-fix/clj-kondo-fix)
(def class-dir "target/classes")
(def native-file "target/clj-kondo-fix")

(defn- version-from
  "accepts a tag such as v0.2.0 or a bare version, from :version or the
  CLJ_KONDO_FIX_VERSION environment variable; dev when neither is set"
  [version]
  (str/replace (str (or version (System/getenv "CLJ_KONDO_FIX_VERSION") "dev")) #"^v" ""))

(def ^:private windows?
  (str/starts-with? (System/getProperty "os.name") "Windows"))

(defn- uber-file [version]
  (format "target/clj-kondo-fix-%s-standalone.jar" version))

(defn- build-uber [{:keys [basis version]}]
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
                              "-Dborkdude.dynaload.aot=true"
                              (str "-Dclj-kondo-fix.version=" version)]})
  (println "Building uberjar...")
  (b/uber {:class-dir class-dir
           :uber-file (uber-file version)
           :basis basis
           :main 'clj-kondo-fix.main})
  (println (str "Done: " (uber-file version))))

(defn uber
  "Build the uberjar. Pass :version v0.2.0 to stamp the version."
  [{:keys [version]}]
  (build-uber {:basis (b/create-basis {:project "deps.edn"})
               :version (version-from version)}))

(defn- find-native-image []
  (let [command (if windows? "native-image.cmd" "native-image")]
    (if-let [graal-home (System/getenv "GRAALVM_HOME")]
      (str graal-home "/bin/" command)
      command)))

(defn native
  "Build the native binary. Pass :version v0.2.0 to stamp the version, and
  :static true for a static musl build (linux amd64 only)."
  [{:keys [version static]}]
  (let [version (version-from version)]
    (build-uber {:basis (b/create-basis {:project "deps.edn" :aliases [:native]})
                 :version version})
    (println "Building native image...")
    (let [args (cond-> [(find-native-image)
                        "-jar" (uber-file version)
                        "-o" native-file
                        "--no-fallback"
                        "--features=clj_easy.graal_build_time.InitClojureClasses"
                        "-H:+ReportExceptionStackTraces"]
                 static (into ["--static" "--libc=musl"]))
          {:keys [exit]} (b/process {:command-args args})]
      (when-not (zero? exit)
        (throw (ex-info "native-image failed" {:exit exit})))
      (println (str "Done: " native-file)))))
