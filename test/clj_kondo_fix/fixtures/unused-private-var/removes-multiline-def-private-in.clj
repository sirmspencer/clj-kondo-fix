(ns foo)

(def ^:private
  default-str
  [:re "^[a-z]+$"])

(defn public [] :ok)
