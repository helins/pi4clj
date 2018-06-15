# pi4clj

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/pi4clj.svg)](https://clojars.org/dvlopt/pi4clj)

Deprecated in favor of
[dvlopt/linux.gpio](https://github.com/dvlopt/linux.gpio.clj) which relies on
the standard Linux API for handling GPIO.

This library was a wrapper around [wiringPi](http://www.wiringpi.com), relying
on the stable JNI bindings provided by [PI4J](http://www.pi4j.com). Although
wiringPi is used and well-known, it is not standard (ie. specific to the
Raspberry Pi) and presents some caveats. For instance, there is no automatic
clean-up. If your program crashes, the state of the GPIO lines remains.

For other capabilities offered by wiringPi :

- [dvlopt/linux.i2c](https://github.com/dvlopt/linux.i2c) for I2C using the
standard Linux API as well.
- [dvlopt/rxtx](https://github.com/dvlopt/rxtx) for serial port IO.

See this [guide](https://github.com/dvlopt/clojure-raspberry-pi) containing
advices and how-to's for running Clojure on the Raspberry Pi.

## Usage

Read the [API](https://dvlopt.github.io/doc/clojure/dvlopt/pi4clj/index.html).

In short :

```clj
(require '[pi4clj.gpio :as gpio])


;; always start your programs by choosing a pin numbering scheme
(gpio/scheme :wiring-pi)


;; let's setup an "interrupt" syncing the state of pin 2 and pin 0
(gpio/edge-handler :sync-pins
                   (fn handler [pin state]
                     (when (= pin
                              2)
                       (gpio/digital 0
                                     state))))


;; declare pin 0 as a digital output with a low value
(-> 0
    (gpio/mode :output/digital)
    (gpio/digital false))


;; declare pin 0 as a digital input with pull resitance set to :down and
;; monitored for edge detection (going from low to high and vice-versa).
(-> 2
    (gpio/mode :input/digital)
    (gpio/pull-resistance :down)
    (gpio/edge-detection :both))
```

## License

Copyright © 2017 Adam Helinski

Distributed under the GNU Lesser General Public License.
