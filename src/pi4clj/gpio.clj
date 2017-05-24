(ns pi4clj.gpio

  "Everything related to GPIO.
  
   Before usage, 'start!' must be called to initialize the board.
   
   Before using a pin, it must be set to a mode with any of the
   'as-...' functions.

   Supported pin modes :
   
      - digital-in
      - digital-out
      - hard-pwm-out
      - soft-pwm-out
      - soft-tone-out
      - gpio-clock
 
   '<!...' functions are for reads
   '>!...' functions are for writes
  
   Digital inputs can be monitored using 'monitor-pin!'.

   The user can register listeners using 'add-listener!' that
   will be invoked everytime the value of any of the monitored pin
   changes."

  (:import (com.pi4j.wiringpi Gpio
                              GpioUtil
                              SoftPwm
                              SoftTone
                              GpioInterrupt
                              GpioInterruptListener
                              GpioInterruptEvent)))





(defn board-revision

  "Get the board revision number"

  []

  (Gpio/piBoardRev))




(def ^:private -*setup-mode
  
  "Keep track of the chosen setup mode.

   Cf. pi4clj.gpio/start!"

  (atom nil))




(defn setup-mode

  "Returns * setup mode chosen by the user at start
           * nil if none has been chosen yet
   
   Cf. pi4clj.gpio/start!"

  []

  @-*setup-mode)




(defmacro ^:private -enforce-setup

  "Execute forms only if there has been a setup.
   Otherwise returns :pi4clj.gpio/not-started."

  [& forms]

  `(if @-*setup-mode
       (do ~@forms)
       :pi4clj.gpio/not-started))




(def ^:private -*monitored-pins
  
  "Keep track of which pins are being monitored for a
   change in value.
   
   Cf. pi4clj.gpio/monitor-pin!"

  (atom #{}))




(defn monitored-pins

  "Get the set containing all the monitored pins.

   Cf. pi4clj.gpio/monitor-pin!"

  []

  @-*monitored-pins)




(defn monitor-pin!

  "Start or stop monitoring for the given pin.
  
   Monitoring means that if the value of the pin changes,
   all user registered listeners will be notified
   
   Cf. pi4clj.gpio/add-listener!

   Consumes 1 thread/pin.

   Returns * true
           * false whether the request succeeded or not

   
   ;; start monitoring pin 2
   (pi4clj.gpio/monitor-pin! 2 true)"

  [pin on?]

  (-enforce-setup
    (locking (str "pi4clj.pin." pin)
      (boolean (if on?
                   (when (and (GpioUtil/setEdgeDetection pin
                                                         GpioUtil/EDGE_BOTH)
                              (pos? (GpioInterrupt/enablePinStateChangeCallback pin)))
                     (swap! -*monitored-pins
                            conj
                            pin)
                     true)
                   (when (pos? (GpioInterrupt/disablePinStateChangeCallback pin))
                     (swap! -*monitored-pins
                            disj
                            pin)
                     true))))))




(defn shutdown-monitoring!

  "Kill all threads related to pin monitoring.
   Useful for a shutdown.

   Cf. pi4clj.gpio/monitor-pin!"

  []
  
  (-enforce-setup
    (doseq [pin @-*monitored-pins]
      (monitor-pin! pin
                    false))
    nil))




(def ^:private -*listeners

  "Map of interrupt listeners registered by the user.
   Each listener is associated with a keyword given by the user.

   Cf. pi4clj.gpio/monitor-pin!
       pi4clj.gpio/add-listener!"
   
  (atom {}))




(defn listeners

  "Get a collection of all the keywords for the registered
   listeners."

  []

  (into #{}
        (keys @-*listeners)))




(defn add-listener!

  "Register a fn to be executed everytime the value of any
   of the monitored pin changes. It is given the pin number
   and the pin state as arguments.

   It is registered with a keyword which can later be
   used to remove the listener.
   
   Returns the keyword.

   Cf. pi4clj.gpio/monitor-pin!

   
   ;; prints the pin number and its current state
   ;; when any monitored pin changes value

   (pi4clj.gpio/add-listener! :print-temperature
                              println)"

  [kw f]

  (swap! -*listeners
         assoc
         kw
         f)
  kw)




(defn remove-listener!

  "Remove the listener registered with the given keyword.
   
   Returns the keyword.
   

   (pi4clj.gpio/remove-listener! :sync-led)"

  [kw]

  (swap! -*listeners
         dissoc
         kw)
  kw)




(defn remove-all-listeners!

  "Remove all listeners.
   
   <!> does not turn off monitoring, merely delete listeners
       (cf. pi4clj/shutdown-monitoring!)"

  []

  (reset! -*listeners
          {}))




(def ^:private -main-listener

  "Listener listening for all interrupts and forwarding to
   the user defined listeners"

  (proxy [GpioInterruptListener] []
    (pinStateChange [^GpioInterruptEvent ev]
      (let [pin   (.getPin   ev)
            state (.getState ev)]
        (doseq [listener (vals @-*listeners)] (listener pin
                                                        state))))))



(defn start!

  "Start GPIO activity. Must be called before using GPIOs.

   3 setup modes are available:

       :abstract    ;; wiringPi abstracted pin numbering (recommended)
       :gpio        ;; broadcom pin numbering
       :sys         ;; same as :gpio but can be executed without sudo
                       slightly slower but more importantly, pins have to
                       be exported and prepared in advance
   
   Once the mode is set, it can't be changed.

   Returns * the given mode
           * another mode if another one has already been set"

  [chosen-mode]

  ;; if needed, call the apropriate setup native function
  ;; and register a single interrupt listener that will call
  ;; user defined listeners from this library

  ;; The :physical mode is not available because it would probably
  ;; break monitoring.

  ;; GpioInterrupt provides native bindings different from wiringPi

  (or @-*setup-mode
      (do (case chosen-mode
            :abstract (Gpio/wiringPiSetup)
            :gpio     (Gpio/wiringPiSetupGpio)
            ;:physical (Gpio/wiringPiSetupPhys)
            :sys      (Gpio/wiringPiSetupSys))
          (reset! -*setup-mode
                  chosen-mode)
          (GpioInterrupt/addListener -main-listener)
          chosen-mode)))




(defn set-pull-resistance

  "Set pull resistance to :off
                          :down
                          :up   for the given digital input.
   
   <!> does not work in :sys numbering mode."

  [pin resistance]
  
  (-enforce-setup
    (Gpio/pullUpDnControl pin
                          (case resistance
                            :off  0
                            :down 1
                            :up   2))))




(defn <!analog

  "Read an int from an analog input.

   WiringPi supports this if an extension is provided.
   Modified versions of wiringPi for other boards might
   have direct support."
  
  [pin]

  (-enforce-setup
    (Gpio/analogRead pin)))




(defn >!analog

  "Write an int to an analog output.

   WiringPi supports this if an extension is provided.
   Modified versions of wiringPi for other boards might
   have direct support."

  [pin value]

  (-enforce-setup
    (Gpio/analogWrite pin value)))




(defn <!digital

  "Read true or false from a digital input, corresponding respectively to
   high and low"

  [pin]

  (-enforce-setup
    (boolean (Gpio/digitalRead pin))))




(defn >!digital
  
  "Set a digital output to true or false, corresponding respectively to high and low"

  [^Long pin state]

  (-enforce-setup
    (Gpio/digitalWrite pin
                       (case state
                         false 0
                         true  1))))




(defn toggle

  "Toggle a digital output and returns the new state"

  [pin]

  (-enforce-setup
    (let [new-state (not (<!digital pin))]
      (>!digital pin
                 new-state)
      new-state)))




(defn >!gpio-clock

  "Sets the frequency of the given gpio clock pin"

  [pin frequency]

  (-enforce-setup
    (Gpio/gpioClockSet pin
                       frequency)))




(defn >!hard-pwm

  "Write an int value to the PWM register for the given pin.
   The Raspberry Pi has a default range of 1024.
   
   Cf. pi4clj.gpio/set-hard-pwm-config"

  [pin value]

  (-enforce-setup
    (Gpio/pwmWrite pin
                   value)))

 


(defn >!soft-pwm

  "Write an int to a software pwm output.
   Within the limits of the declared range.
   
   Cf. pi4clj.gpio/as-soft-pwm-out"

  [pin value]

  (-enforce-setup
    (SoftPwm/softPwmWrite pin
                          value)))




(defn >!soft-tone

  "Change the frequence of the given software tone pin.
   
   Maximum is 5000."

  [pin frequency]

  (-enforce-setup
    (SoftTone/softToneWrite pin
                            frequency)))




(defn as-digital-out

  "Declare the given pin as a digital output.

   Optionally accepts true or false as a start state, corresponding respectively
   to high and low.
   
   Cf. pi4clj.gpio/>!digital


   ;; pin 0 set to high

   (pi4clj.gpio/as-digital-out 0
                               true)"

  [pin & [state]]

  (-enforce-setup
    (Gpio/pinMode pin
                  Gpio/OUTPUT)
    (when state
      (>!digital pin
                 state))
    nil))




(defn as-digital-in

  "Declare the given pin as a digital input.

   Can set pull resistance via :pull
           monitoring      via :monitor?

   Cf. pi4clj.gpio/set-pull-resistance
       pi4clj.gpio/monitor-pin!


   ;; pin 2, monitored and pulled-down
   
   (pi4clj.gpio/as-digital-in 2
                              :pull       :down
                              :monitored? true)"

  [pin & {:keys [pull
                 monitor?]}]

  (-enforce-setup
    (Gpio/pinMode pin
                  Gpio/INPUT)
    (when-not (nil? pull)
      (set-pull-resistance pin
                           pull))
    (when-not (nil? monitor?)
      (monitor-pin! pin
                    monitor?))
    nil))




(defn as-gpio-clock

  "Declare the given pin as gpio clock.

   Optionally accepts a frequency start value.

   Cf. pi4clj.gpio/>!gpio-clock"

  [pin & [frequency]]

  (-enforce-setup
    (Gpio/pinMode pin
                  Gpio/GPIO_CLOCK)
    (when frequency
      (>!gpio-clock pin
                    frequency))))




(defn as-hard-pwm-out

  "Declare the given pin as a hardware pwm output.
   An optional start value can be provided.
   
   Cf. pi4clj.gpio/set-hard-pwm-config
       pi4clj.gpio/>!hard-pwm"

  [pin & [value]]

  (-enforce-setup
    (Gpio/pinMode pin
                  Gpio/PWM_OUTPUT)
    (when value
      (>!hard-pwm pin
                  value))
    nil))




(defn as-soft-pwm-out

  "Declare the given pin as software pwm output.
   
   Creates thread and emulates mark:space pwm.
   The pulse width is fixed at 100µs for efficiency.
   
   Unlike hardware pwm outputs, the range can be
   different for each pin. The default and recommended
   range is 100.

   The frequency is a function of the pulse width and
   the range :

       recommended range x pulse width = period

       100 x 100µs = 10 000µs  =>  100Hz
   
   Thus, for a higher frequency at the expense of resolution,
   decrease range.
   
   Cf. pi4clj.gpio/set-hard-pwm-config
   
   
   ;; pin 3 with a value of 50 and a default range of 100
   (pi4clj.gpio/as-soft-pwm-out! 3 :value 50)"

  [pin & {pwm-range :range
          pwm-value :value
          :or   {pwm-range 100
                 pwm-value   0}}]
  
  (-enforce-setup
    (SoftTone/softToneStop pin)
    (SoftPwm/softPwmCreate pin
                           pwm-value
                           pwm-range)
    nil))




(defn stop-soft-pwm

  "Stop using the given pin as a software pwm output
   and kill the related thread.
   
   Cf. pi4clj.gpio/as-soft-pwm-out"

  [pin]

  (-enforce-setup
    (SoftPwm/softPwmStop pin)))




(defn as-soft-tone-out

  "Declare the given pin as a software tone output.
   
   Create a thread and emulate a tone output.
   
   For efficiency reasons, the pulse width is 100µs.
   Hence the maximum frequence is :

       1 / 0.0002 = 5KHz"
   

  [pin & [frequency]]

  (-enforce-setup
    (SoftPwm/softPwmStop pin)
    (SoftTone/softToneCreate pin)
    (when frequency
      (SoftTone/softToneWrite pin
                              frequency))))




(defn stop-soft-tone

  "Stop using the given pin as a software tone output
   and kill the related thread.

   Cf. pi4clj.gpio/as-soft-tone-out"

  [pin]

  (-enforce-setup
    (SoftTone/softToneStop pin)))
          


(defn set-hard-pwm-config

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

   <!> Doesn't work in :sys setup mode."

  [& {pwm-mode  :mode
      divisor   :clock-divisor
      pwm-range :range}]

  (-enforce-setup
    (when pwm-mode
      (Gpio/pwmSetMode (case pwm-mode
                         :balanced   Gpio/PWM_MODE_BAL
                         :mark-space Gpio/PWM_MODE_MS)))
    (when divisor
      (Gpio/pwmSetClock divisor))
    (when pwm-range
      (Gpio/pwmSetRange pwm-range))
    nil))
