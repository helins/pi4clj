(ns pi4clj.examples.sync

  "Sync the state of an input pin, such as a button, with the state of an output pin, such as a LED."

  {:author "Adam Helinski"}

  (:require [pi4clj.gpio :as gpio]))




;;;;;;;;;;


(defn setup

  "Cf. namespace description"

  ([]

   (setup nil))


  ([{:as   config
     :keys [scheme
            pin-input
            pin-output]
     :or   {scheme     :wiring-pi
            pin-input  0
            pin-output 1}}]
 
   (gpio/scheme scheme)
   (gpio/edge-handler ::sync-pins
                      (fn handler [pin state]
                        (when (= pin
                                 2)
                          (gpio/digital 0
                                        state))))
   (-> pin-input
       (gpio/mode :input/digital)
       (gpio/pull-resistance :down)
       (gpio/edge-detection :both))
   (-> pin-output
       (gpio/mode :output/digital)
       (gpio/digital false))
   nil))




(defn stop

  "Stops the synchronization."

  ([]

   (stop nil))

  
  ([{:as   config
     :keys [pin-input
            pin-output]
     :or   {pin-input  0
            pin-output 1}}]

   (gpio/edge-handler ::sync-pins
                      nil)
   (gpio/edge-detection pin-input
                        :none)
   (gpio/digital pin-output
                 false)
   nil))
