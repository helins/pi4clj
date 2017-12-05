(ns pi4clj.gpio.emulated

  "Handles software emulated GPIO.
  
   Of course, it is less efficient and precise than hardware GPIO, but can be useful."

  {:author "Adam Helinski"}

  (:require [pi4clj.gpio :as gpio])
  (:import (com.pi4j.wiringpi SoftPwm
                              SoftTone)))




;;;;;;;;;; Private


(defn- -pwm-output

  "Helper for `mode`."

  ([pin]

   (-pwm-output pin
                nil))


  ([pin {:as   config
         :keys [range
                value]
         :or   {range 100
                value 0}}]
   
   (SoftTone/softToneStop pin)
   (SoftPwm/softPwmCreate pin
                          value
                          range)
   pin))




(defn- -tone-output

  "Helper for `mode`."
   
  [pin]

  (SoftPwm/softPwmStop pin)
  (SoftTone/softToneCreate pin))




;;;;;;;;;;


(defn mode

  "Sets the emulated mode of a pin.

   <!> Consumes 1 thread / emulated pin.


   @ pin
     Pin number.


   @ mode
     One of :

       :none
        Stop any emulation if there is on going on.


       :output/pwm
        Software emulated PWM output in mark:space mode.

        Configurable with a map such as :

          {:range
            Unlike hardware PWM ouputs, the range can be different for each
            pin (default is 100, recommended).

            The frequency is a function of the pulse width and the range :

              recommended range x pulse width = period
 
              100 x 100µs = 10 000µs  =>  100Hz
   
            Thus, for a higher frequency at the expense of resolution, decrease
            range.

            Cf. pi4clj.gpio/config-pwm

           :value
            Initial value.}


       :output/tone
        Software emulated tone output.

        For efficiency reasons, the pulse width is 100µs.
        Hence the maximum frequence is :

            1 / 0.0002 = 5KHz


    @ config
      Cf. @ mode


    => `pin`"

  ([pin mode]

   (mode pin
         mode
         nil))


  ([pin mode config]

   (condp identical?
          mode
     :none        (do (SoftPwm/softPwmStop pin)
                      (SoftTone/softToneStop pin))
     :output/pwm  (-pwm-output pin
                               config)
     :output/tone (-tone-output pin))
   pin))




(defn pwm

  "Writes a value to an emulated PWM output.

   The Raspberry Pi has a default range of 1024.
   
  
   @ pin
     Pin number.

   @ value
     Value considering the PWM range set for the given pin.

   => `pin`


   Cf. `pwm-config`"

  [pin value]

  (SoftPwm/softPwmWrite pin
                        value)
  pin)




(defn tone

  "Sets the frequency of the given software emulated tone pin.

   @ pin
     Pin number.

   @ frequency
     In Hertz, between 0 and 5000.

   => `pin`"

  [pin frequency]

  (SoftTone/softToneWrite pin
                          frequency)
  pin)
