(ns pi4clj.gpio

  "Handles hardware GPIO.

   <!> Prior to any I/O operation, a pin numbering scheme must be chosen.
       Cf. `scheme`"

  (:import (com.pi4j.wiringpi Gpio
                              GpioUtil
                              GpioInterrupt
                              GpioInterruptListener
                              GpioInterruptEvent)))




;;;;;;;;;; Private


(defn- -enforce-root

  "Throws an exception if not running as root."

  []

  (when (not= (System/getProperty "user.name")
              "root")
    (throw (RuntimeException. "Must run as root"))))




(def ^:private -*scheme
  
  "Atom tracking if a pin numbering scheme has been chosen.

   Cf. `scheme`"

  (atom false))




(def ^:private -*edge-detection

  "Atom tracking edge detection, ie. a map of pin numbers to edge modes.
   
   Cf. `edge-detection`"

  (atom #{}))




(def ^:private -*edge-handlers

  "Atom containing edge handlers, ie. a map of keywords to fns registesred by the user.

   Cf. `edge-detection`
       `edge-handler`"
   
  (atom {}))




;;;;;;;;;; Stop edge detection on JVM shutdown


(declare edge-detection)

(.addShutdownHook (Runtime/getRuntime)
                  (Thread. ^Runnable (fn shutdown-edge-detection []
                                       (doseq [[pin] @-*edge-detection]
                                         (edge-detection pin
                                                         :none))
                                       (when-not (empty? @-*edge-detection)
                                         (recur)))))




;;;;;;;;;; API - Setup


(defn scheme

  "Starts GPIO activity by choosing a pin numbering scheme, must be called prior to doing IO.

   Once a scheme is chosen, it cannot be changed. Calling this function again won't do anything.


   @ scheme
     One of these numbering schemes :

       :wiring-pi
        WiringPi abstracted pin numbering, consistent across models and revisions (recommended and default).
        Must run as root.

       :broadom
        Broadcom pin numbering, might and will change across models and revisions.
        Must run as root.

       :sys
        Same as :broadcom but uses the file system instead of writing directly to '/dev/mem'.
        Hence, it is a lot slower and not every fn from this library work in this mode.
        Plus, pins have to be exported and prepared in advance.

   => Applied scheme."

  ([]

   (scheme :wiring-pi))


  ([scheme]

   (locking -*scheme
     (when (nil? @-*scheme)
       (when (#{:wiring-pi
                :broadcom} scheme)
         (-enforce-root))
       (case scheme
         :wiring-pi (Gpio/wiringPiSetup)
         :broadcom  (Gpio/wiringPiSetupGpio)
         :sys       (Gpio/wiringPiSetupSys))
       (reset! -*scheme
               scheme)
       ;; GpioInterrupt provides better native bindings than wiringPi
       (GpioInterrupt/addListener
         (proxy [GpioInterruptListener] []
           (pinStateChange [^GpioInterruptEvent event]
             (let [pin   (.getPin   event)
                   state (.getState event)]
               (doseq [listener (vals @-*edge-handlers)]
                 (listener pin
                           state)))))))
     @-*scheme)))




(defn mode

  "Sets the mode of a pin.

   @ pin
     Pin number.

   @ mode
     One of #{:input/digital
              :output/digital
              :output/pwm
              :outpuw/gpio-clock}

   => pin"

  [pin mode]

  (Gpio/pinMode pin
                (condp identical?
                       mode
                  :input/digital     Gpio/INPUT
                  :output/digital    Gpio/OUTPUT
                  :output/pwm        Gpio/PWM_OUTPUT
                  :output/gpio-clock Gpio/GPIO_CLOCK))
  pin)




(defn pull-resistance

  "Sets the pull-resistance of a digital input.

   <!> Does not work in :sys numbering scheme.


   @ pin
     Pin number.

   @ resistance
     One of #{:off
              :down
              :up}

   => `pin`"

  [pin resistance]
  
  (Gpio/pullUpDnControl pin
                        (case resistance
                          :off  0
                          :down 1
                          :up   2))
  pin)




(defn pwm-config

  "Modifies how hardware pwm outputs behaves.

   <!> Does not work in :sys numbering scheme.


   @ ?config
     {:?mode
       Two pwm modes are available, modifying how the duty cycle is modeled.
       Ex. Let us say a duty cycle of 25% :

           :mark-space
           __      __
             ______  ______

           :balanced (default)
           _   _   _   _
            ___ ___ ___ ___


      :?clock-divisor
       PWM clock, base is 19.2MHz.

      :?range
       The pwm clock increments a counter at each pulse. When this counter hits
       the value given by the range (default is 1024), it resets to 0 and it is
       equivalent to a period. The range is therefore the number of pulses per
       period.}

   => nil


   duty cycle = range / value

   In :balanced mode, the frequency varies with the duty cycle.

   In :mark-space mode, a higher range results in a better resolution but a lower
   frequency as such :

       frequency = 19.2MHz / clock-divisor / range"


  [{:as   ?config
    :keys [?mode
           ?clock-divisor
           ?range]}]

  (when ?mode
    (Gpio/pwmSetMode (case mode
                       :balanced   Gpio/PWM_MODE_BAL
                       :mark-space Gpio/PWM_MODE_MS)))
  (when ?clock-divisor
    (Gpio/pwmSetClock ?clock-divisor))
  (when ?range
    (Gpio/pwmSetRange ?range))
  nil)




;;;;;;;;;; API - Do I/O


(defn analog

  "Reads or writes an analog pin.

   WiringPi supports this if an extension is provided.

   Modified versions of wiringPi for other boards might have direct support.
  
   -----

   For reading :

     @ pin
       Pin number.

     => Integer.

   -----

   For writing :

     @ pin
       Pin number

     @ value
       Integer.

     => `pin`"
  
  ([pin]

   (Gpio/analogRead pin))


  ([pin value]

   (Gpio/analogWrite pin
                     value)
   pin))




(defn digital

  "Reads or writes a digital pin using booleans.

     True  represents a HIGH value.
     False represents a LOW  value.
  
  -----

  For reading :

    @ pin
      Pin number.

    => Boolean

  -----

  For writing :

    @ pin
      Pin number.

    @ state
      Boolean.

    => `pin`"

  ([pin]

   (not (zero? (Gpio/digitalRead pin))))


  ([^long pin state]

   (Gpio/digitalWrite pin
                      (case state
                        false 0
                        true  1))
   pin))




(defn pwm

  "Writes a value to a PWM output.

   The Raspberry Pi has a default range of 1024.
   
  
   @ pin
     Pin number.

   @ value
     Value considering the PWM range.

   => `pin`


   Cf. `pwm-config`"

  [pin value]

  (Gpio/pwmWrite pin
                 value))




(defn gpio-clock

  "Sets the frequency of the given gpio clock pin.
  
   @ pin
     Pin number.
  
   @ frequency
     In Hertz.
  
   => `pin`"

  [pin frequency]

  (Gpio/gpioClockSet pin
                     frequency)
  pin)




;;;;;;;;;; API - Edge detection and "interrupts"


(defn edge-detection

  "Sets edge detection, ie. monitoring a pin for state change.

   In case of a high number of state changes happening very fast, the file system will not keep up,
   meaning it will drop some (actually, most of them). For lower throughput, there should not be
   any problem.

   If genuine interrupts are needed for a high throughput, other solutions must be considered
   such as using a microcontroller.

   <!> Consumes 1 thread / pin.


   @ pin
     Pin number.

   @ mode
     One of :

       :none
        Disables monitoring, kills the associated thread is there is one.

       :rising
        Monitors only when to pin goes from low to high.

       :falling
        Monitors only when the pin goes from high to low.

       :both
        Monitors both.

   => true
        If the operation succeeded.

      false
        If not, meaning setting edge detection failed.


   Ex. (gpio/edge-detection 2
                            :rising)


   Cf. `edge-handler`"

  [pin mode]

  (if (identical? mode
                  :none)
    (if (pos? (GpioInterrupt/disablePinStateChangeCallback pin))
      (do (swap! -*edge-detection
                 dissoc
                 pin)
          true)
      false)
    (if (and (GpioUtil/setEdgeDetection pin
                                        (condp identical?
                                               mode
                                          :rising  GpioUtil/EDGE_RISING
                                          :falling GpioUtil/EDGE_FALLING
                                          :both    GpioUtil/EDGE_BOTH))
             (pos? (GpioInterrupt/enablePinStateChangeCallback pin)))
      (do (swap! -*edge-detection
                 assoc
                 pin
                 mode)
          true)
      false)))




(defn edge-handler

  "Registers or removes an edge handler.

   A handler reacts to anything resulting from `edge-detection`. Hence, it is easy
   to write handler reacting to several pins.


   @ key
     Key for the given handler.

   @ ?handler
     One of :

       nil
         For removing the current handler.
    
       (fn [pin state])
         For registering a new one (and overwriting what already exists).
         In case of a high number of events happening very fast, the state might be wrong.
         Otherwise, the user should assume the state is correct.

   => `key`
  

   Ex. (edge-handler :button
                     (fn handler [pin value]
                       (when (and (= pin
                                     2))
                         ...)))"

  [key ?handler]

  (swap! -*edge-handlers
         (fn swap-handlers [handlers]
           (cond
             (fn? ?handler)  (assoc handlers
                                    key
                                    ?handler)
             (nil? ?handler) (dissoc handlers
                                     key)
             :else           handlers)))
  key)




;;;;;;;;;; API - Miscellaneous


(defn board-revision

  "Gets the board revision number."

  []

  (Gpio/piBoardRev))
