(ns pi4clj.gpio

  "Everything related to hardware GPIO. 

   Prior to any IO operation, a pin numbering scheme must be chosen
   (cf. scheme).
  
   Inputs and outputs have to be declared via functions :

      - digital-in
      - digital-out
      - pwm-out
      - gpio-clock

   rd-xxxx functions are for reading from an input.
   wr-xxxx functions are for writing to an output.

   A pin can be monitored for any change in value (cf. monitor).
   The user can register listeners reacting to those changes (cf. listen)."

  (:import (com.pi4j.wiringpi Gpio
                              GpioUtil
                              GpioInterrupt
                              GpioInterruptListener
                              GpioInterruptEvent)))





(defn board-revision

  "Get the board revision number"

  []

  (Gpio/piBoardRev))




(defn- -enforce-root

  "Throw an exception if not running as root"

  []

  (when (not= (System/getProperty "user.name")
              "root")
    (throw (RuntimeException. "Must run as root"))))




(def ^:private -*listeners

  "Map of keywords -> interrupt listeners registered by the user.

   Cf. monitor 
       listen"
   
  (atom {}))




(defn listeners

  "Get a set of all the registered listeners.

   Cf. listen"

  []

  (into #{}
        (keys @-*listeners)))




(defn listen

  "A listener is a function accepting a pin number and its value as
   arguments. It is associated with a kewyord and is called everytime
   the value of a monitored pin changes.

   In this fashion, it is easy to register multiple listeners for a single
   pin or listeners reacting to several ones.

   In order to remove one, explicitly provide nil instead of a function.

   Cf. monitor
   

   ;; do something when the relevant button is high
   (pi4clj.gpio/listen :button
                       (fn [pin value]
                         (when (and (= pin 2)
                                    value)
                           (do-something))))"

  [kw f]

  (swap! -*listeners
         #(if f
              (assoc %
                     kw
                     f)
              (dissoc %
                      kw)))
  kw)




(def ^:private -*scheme?
  
  "Has a pin numbering pin been chosen ?

   Cf. scheme"

  (atom false))




(defn scheme

  "Start GPIO activity by choosing a pin numbering scheme.

   3 numbering schemes are available :

       :wiring-pi   ;; wiringPi abstracted pin numbering, consistent across
                       models and revisions (recommended)
       :broadom     ;; broadcom pin numbering, might and will change across
                       models and revisions
       :sys         ;; same as :broadcom but can be executed without sudo

   <!> :wiring-pi and :broadcom must be run as root because IO functions are
       writing directly to /dev/mem.
  
       :sys doesn't need to but is slower, not every IO functions is available
       and pins have to be exported and prepared in advance. It is recommended
       only when the program cannot run as root and IO is not too complex.

   Once a scheme is chosen, it cannot be changed. Calling this function again
   won't do anything.

   IO functions always enforce that a scheme is chosen. If there isn't any, this
   function will be called with the :wiring-pi scheme. Nonetheless, it is good
   practise to always explicitly call this function at the start of the program."

  [scheme]

  ;; GpioInterrupt provides better native bindings than wiringPi

  (when-not @-*scheme? 
    (if (compare-and-set! -*scheme?
                          false
                          true)
        (do (when (#{:wiring-pi
                     :broadcom} scheme)
              (-enforce-root))
            (case scheme
              :wiring-pi (Gpio/wiringPiSetup)
              :broadcom  (Gpio/wiringPiSetupGpio)
              :sys       (Gpio/wiringPiSetupSys))
            (GpioInterrupt/addListener
              (proxy [GpioInterruptListener] []
                (pinStateChange [^GpioInterruptEvent ev]
                  (let [pin   (.getPin   ev)
                        state (.getState ev)]
                    (doseq [listener (vals @-*listeners)] (listener pin
                                                                    state))))))
            nil)
        (recur scheme))))
  



(defmacro enforce-scheme

  "Execute the given forms after ensuring that a numbering scheme
   has been chosen, defaulting to :wiring-pi if none has been
   explicitly selected.

   Cf. scheme"

  [& forms]

  `(do (scheme :wiring-pi)
       ~@forms))




(def ^:private -*monitored
  
  "All the pins that are being monitored for a change in value.
   
   Cf. monitor"

  (atom #{}))




(defn monitored

  "Get a set containing all the monitored pins.

   Cf. monitor"

  []

  @-*monitored)




(defn monitor

  "Start or stop monitoring  the given pin.
  
   Monitoring means that if the value of the pin changes, all users registered
   listeners will be called.

   Returns a boolean indicating whether the request succeeded or not.

   <!> Consumes 1 thread/pin.

   Cf. listen
   
   ;; start monitoring pin 2
   (pi4clj.gpio/monitor 2 true)"

  [pin on?]

  (enforce-scheme
    (locking (str "pi4clj.pin." pin)
      (boolean (if on?
                   (when (and (GpioUtil/setEdgeDetection pin
                                                         GpioUtil/EDGE_BOTH)
                              (pos? (GpioInterrupt/enablePinStateChangeCallback pin)))
                     (swap! -*monitored
                            conj
                            pin)
                     true)
                   (when (pos? (GpioInterrupt/disablePinStateChangeCallback pin))
                     (swap! -*monitored
                            disj
                            pin)
                     true))))))




(defn shutdown-monitoring

  "Kill all threads related to pin monitoring.

   Useful for a shutdown.

   Cf. monitor"

  []
  
  (enforce-scheme
    (doseq [pin @-*monitored]
      (monitor pin
               false))
    (if (empty? @-*monitored)
        nil
        (recur))))




;; Analog


(defn rd-analog

  "Read an int from an analog input.

   WiringPi supports this if an extension is provided.
   Modified versions of wiringPi for other boards might
   have direct support."
  
  [pin]

  (enforce-scheme
    (Gpio/analogRead pin)))




(defn wr-analog

  "Write an int to an analog output.

   WiringPi supports this if an extension is provided.
   Modified versions of wiringPi for other boards might
   have direct support."

  [pin value]

  (enforce-scheme
    (Gpio/analogWrite pin value)))




;; Digital


(defn rd-digital

  "Read true or false from a digital input, corresponding respectively to
   high and low"

  [pin]

  (enforce-scheme
    (not (zero? (Gpio/digitalRead pin)))))




(defn wr-digital
  
  "Set a digital output to true or false, corresponding respectively to high and low"

  [^Long pin state]

  (enforce-scheme
    (Gpio/digitalWrite pin
                       (case state
                         false 0
                         true  1))))




(defn tg-digital

  "Toggle a digital output and return the new state"

  [pin]

  (enforce-scheme
    (let [new-state (not (rd-digital pin))]
      (wr-digital pin
                  new-state)
      new-state)))




(defn digital-out

  "Declare the given pin as a digital output.

   Optionally accepts true or false as a start value.
   
   Cf. pi4clj.gpio/wr-digital


   ;; pin 0 set to high

   (pi4clj.gpio/digital-out 0
                            true)"

  [pin & [state]]

  (enforce-scheme
    (Gpio/pinMode pin
                  Gpio/OUTPUT)
    (when state
      (wr-digital pin
                  state))
    nil))




(defn pull-resistance

  "Set pull resistance to :off
                          :down
                          :up   for the given digital input.
   
   <!> Does not work in :sys numbering scheme."

  [pin resistance]
  
  (enforce-scheme
    (Gpio/pullUpDnControl pin
                          (case resistance
                            :off  0
                            :down 1
                            :up   2))))



(defn digital-in

  "Declare the given pin as a digital input.

   Can set pull resistance via :pull
           monitoring      via :monitor?

   Cf. pi4clj.gpio/pull-resistance
       pi4clj.gpio/monitor


   ;; pin 2, monitored and pulled-down
   
   (pi4clj.gpio/digital-in 2
                           {:pull       :down
                            :monitored? true})"

  [pin & [{:keys [pull
                  monitor?]}]]

  (enforce-scheme
    (Gpio/pinMode pin
                  Gpio/INPUT)
    (when-not (nil? pull)
      (pull-resistance pin
                       pull))
    (when-not (nil? monitor?)
      (monitor pin
               monitor?))
    nil))




;; PWM


(defn config-pwm

  "Hardware pwm outputs can be modified by 3 options :
   
       - :mode
       - :clock-divisor
       - :range

   These options modify the behavior of all the hardware
   pwm outputs.

   Two pwm modes are available, modifying how the duty cycle
   is modeled (25% in this example) :

       :mark-space
       __      __
         ______  ______

       :balanced (default)
       _   _   _   _
        ___ ___ ___ ___
   
   The pwm clock increments a counter at each pulse. When
   this counter hits a value specified by the range, it
   resets to 0 and it is equivalent to a period. The range
   is therefore the number of pulses per period (default to
   1024).
  
   On the Raspberry Pi, the base pwm clock is 19.2MHz

       duty cycle = range / value

   In :balanced mode, the frequency varies with the duty cycle.

   In :mark-space mode, a higher range results in a better
   resolution but a lower frequency as such :

       frequency = 19.2MHz / clock-divisor / range

   <!> Does not work in :sys numbering scheme."

  [& [{:as   config
       :keys [mode
              clock-divisor
              range]}]]

  (enforce-scheme
    (when mode
      (Gpio/pwmSetMode (case mode
                         :balanced   Gpio/PWM_MODE_BAL
                         :mark-space Gpio/PWM_MODE_MS)))
    (when clock-divisor
      (Gpio/pwmSetClock clock-divisor))
    (when range
      (Gpio/pwmSetRange range))
    config))




(defn wr-pwm

  "Write an int value to the PWM register for the given pin.
   The Raspberry Pi has a default range of 1024.
   
   Cf. pi4clj.gpio/config-pwm"

  [pin value]

  (enforce-scheme
    (Gpio/pwmWrite pin
                   value)))




(defn pwm-out

  "Declare the given pin as a pwm output.
   An optional start value can be provided.
   
   Cf. pi4clj.gpio/config-pwm
       pi4clj.gpio/wr-pwm"

  [pin & [value]]

  (enforce-scheme
    (Gpio/pinMode pin
                  Gpio/PWM_OUTPUT)
    (when value
      (wr-pwm pin
              value))
    nil))




;; Gpio clock


(defn wr-gpio-clock

  "Set the frequency of the given gpio clock pin"

  [pin frequency]

  (enforce-scheme
    (Gpio/gpioClockSet pin
                       frequency)))




(defn gpio-clock

  "Declare the given pin as gpio clock.

   Optionally accepts a frequency start value in Hertz.

   Cf. pi4clj.gpio/wr-gpio-clock"

  [pin & [frequency]]

  (enforce-scheme
    (Gpio/pinMode pin
                  Gpio/GPIO_CLOCK)
    (when frequency
      (wr-gpio-clock pin
                     frequency))))
