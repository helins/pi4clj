(ns gpio-example.core

  "A led connected on pin 0 will sync with a button connected
   to pin 2. When :low, the led will be :low, and similarly when
   :high."

  (:require [pi4clj.gpio :as gpio]))



(defn -main

  [& args]

  (gpio/add-listener! :sync
                      (fn [pin state]
                        (when (= pin 2)
                          (gpio/>!digital 0
                                          state))))
  (gpio/start! :abstract)
  (gpio/as-digital-out 0
                       false)
  (gpio/as-digital-in 2
                      :pull     :down
                      :monitor? true))
