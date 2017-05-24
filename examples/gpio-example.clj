(ns gpio-example.core

  "A led connected to pin 0 will sync with a button connected
   to pin 2. When low, the led will be low, and similarly when
   high."

  (:require [pi4clj.gpio :as gpio]))





(defn -main

  [& args]

  (gpio/listen :sync
               (fn [pin state]
                 (when (= pin 2)
                   (gpio/wr-digital 0
                                    state))))
  (gpio/scheme :wiring-pi)
  (gpio/digital-out 0
                    false)
  (gpio/digital-in 2
                   {:pull     :down
                    :monitor? true}))
