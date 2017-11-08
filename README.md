# pi4clj

![Raspberry Pi](http://i.imgur.com/FeW5Q.png)

Handles GPIOs (and more to come) on the Raspberry Pi and similar boards.

Uses the JNI bindings to the excellent [wiringPi](http://www.wiringpi.com)
provided by [PI4J](http://www.pi4j.com). No black magic involved.

For [I²C](https://en.wikipedia.org/wiki/I%C2%B2C), refer to
[Icare](https://github.com/dvlopt/icare), a multi platform clojure library.

## Installation

No prior installation is required, simply add this to your dependencies :
```clj
[dvlopt/pi4clj "0.0.0-alpha6"]
```

Interactive development on the board itself is a bliss. Nonetheless, lein's repl can be very slow
to start and might even timeout. Simply set a higher timeout than the default 30s in your project file :
```clj
{:repl-options {:timeout 120000}}    ;; 2 minutes
```

OpenJDK isn't optimized for ARM at all and is an order of magnitude slower than Oracle's JDK. On
the other hand, Oracle's JDK... well, in one word, "licensing". That's why we recommend the very
promising [Zulu Embedded](https://www.azul.com/products/zulu-embedded/) for running java and clojure
on the Raspberry Pi. For installation, go [there](https://blog.benjamin-cabe.com/2016/04/05/installing-the-zulu-open-source-java-virtual-machine-on-raspberry-pi).

## Usage

Read the full [API](https://dvlopt.github.io/doc/pi4clj).

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


;; declare pin 0 as a digital input with pull resitance set to :down and monitored
;; for edge detection (going from false to true and vice-versa).
(-> 2
    (gpio/mode :input/digital)
    (gpio/pull-resistance :down)
    (gpio/edge-detection :both))
```

## Status

In alpha, breaking changes might occur. Other than that, it is already being used in production.

More capabilities, such as SPI and shift registers, will be added when needed.

While PI4J supports other boards than the Raspberry family, this library hasn't been tested for
anything else. Because it is lightweight, it should work just fine.

## License

Copyright © 2017 Adam Helinski

Distributed under the GNU Lesser General Public License.
