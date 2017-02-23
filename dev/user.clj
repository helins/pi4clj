(ns user
  (:require [clojure.tools.namespace.repl :as repl]
            ;[clojure.spec                :as s]
            ;[clojure.spec.gen            :as gen]
            ;[clojure.spec.test           :as stest]
            [pi4clj.gpio :as gpio]
            [pi4clj.i2c  :as i2c]
            )
  (:gen-class))




(def rl
     "Reload modified namespaces"
     repl/refresh)


(def rla
     "Reload all namespaces"
     repl/refresh-all)




(comment





)
