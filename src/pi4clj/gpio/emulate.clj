(ns pi4clj.gpio.emulate

  "Everything related to software emulated GPIO.

   Typically, uses some form of bit banging on a reserved thread."

  {:author "Adam Helinski"}

  (:require [pi4clj.gpio :as gpio])
  (:import (com.pi4j.wiringpi SoftPwm
                              SoftTone)))





(defn wr-pwm

  "Write an int to a software emulated pwm output.
   Within the limits of the declared range.
   
   Cf. pwm-out"

  [pin value]

  (gpio/enforce-scheme
    (SoftPwm/softPwmWrite pin
                          value)))




(defn pwm-out

  "Declare the given pin as software emulated pwm output in
   mark:space mode.
   
   Unlike hardware pwm outputs, the range can be
   different for each pin. The default and recommended
   range is 100.

   The frequency is a function of the pulse width and
   the range :

       recommended range x pulse width = period

       100 x 100µs = 10 000µs  =>  100Hz
   
   Thus, for a higher frequency at the expense of resolution,
   decrease range.

   <!> Uses 1 thread per emulated pin.
   
   Cf. pi4clj.gpio/config-pwm
   
   
   ;; pin 3 with a value of 50 and a default range of 100
   (pi4clj.gpio/pwm-out 3 {:value 50})"

  [pin & [{:as   config
           :keys [range
                  value]
           :or   {range 100
                  value   0}}]]
  
  (gpio/enforce-scheme
    (SoftTone/softToneStop pin)
    (SoftPwm/softPwmCreate pin
                           value
                           range)
    pin))




(defn wr-soft-tone

  "Set the frequency in Hertz of the given software tone pin.
   
   Maximum is 5000."

  [pin frequency]

  (gpio/enforce-scheme
    (SoftTone/softToneWrite pin
                            frequency)))




(defn tone-out

  "Declare the given pin as a software emulated tone output.
   
   For efficiency reasons, the pulse width is 100µs.
   Hence the maximum frequence is :

       1 / 0.0002 = 5KHz


   <!> Uses 1 thread per emulated pin.


   ;; pin 3 with a frequency of 5 Hertz
   (pi4clj.gpio.emulated/tone-out 3 5)"
   
   

  [pin & [frequency]]

  (gpio/enforce-scheme
    (SoftPwm/softPwmStop pin)
    (SoftTone/softToneCreate pin)
    (when frequency
      (SoftTone/softToneWrite pin
                              frequency))))




(defn stop

  "Stop emulating a pin by killing the related thread.

   Cf. pwm-out
       tone-out"

  [pin]

  (gpio/enforce-scheme
    (SoftPwm/softPwmStop pin)
    (SoftTone/softToneStop pin)
    pin))
