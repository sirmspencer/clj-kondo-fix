;;-; middle var removed from :refer vector; space between flanking vars preserved ;-;;
(ns foo (:require [burpless :refer [step run-cucumber hook]]))

(step) (hook)
