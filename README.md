# pi4clj

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/pi4clj.svg)](https://clojars.org/dvlopt/pi4clj)

Handle GPIOs on the Raspberry Pi and similar boards.

This library relies on the stable JNI bindings to the excellent
[wiringPi](http://www.wiringpi.com) provided by [PI4J](http://www.pi4j.com). No
black magic involved.

For using [I2C](https://en.wikipedia.org/wiki/I%C2%B2C), refer to
[dvlopt.i2c](https://github.com/dvlopt/i2c).

For using the serial port, refer to [Clotty](https://github.com/dvlopt/clotty),
a multi plaform clojure wrapper around [RxTx](https://github.com/openmuc/jrxtx).

See this [guide](https://github.com/dvlopt/clojure-raspberry-pi) containg
advices and how-to's for running Clojure on the Raspberry Pi.

## Usage

Read the [API](https://dvlopt.github.io/doc/pi4clj).

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

## Status

In alpha, breaking changes might occur. Other than that, it is already being
used in production.

While PI4J supports other boards than the Raspberry family, this library has not
been tested for anything else. Because it is lightweight, it should work just
fine.

## License

Copyright © 2017-2018 Adam Helinski

Distributed under the GNU Lesser General Public License.
