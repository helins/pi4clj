# pi4clj

Handles GPIOs (and more to come) on the Raspberry Pi and similar boards.

Uses the JNI bindings to the excellent [wiringPi](http://www.wiringpi.com)
provided by [PI4J](http://www.pi4j.com). No black magic involved.

For using [I²C](https://en.wikipedia.org/wiki/I%C2%B2C), refer to
[Icare](https://github.com/dvlopt/icare), a multi platform clojure library.

For using the serial port, refer to [Clotty](https://github.com/dvlopt/clotty),
a multi plaform clojure wrapper around [RxTx](https://github.com/openmuc/jrxtx).

## Installation

No prior installation is required, simply add this to your dependencies :

[![Clojars
Project](https://img.shields.io/clojars/v/dvlopt/pi4clj.svg)](https://clojars.org/dvlopt/pi4clj)

Interactive development on the board itself is a bliss but the repl is very slow
to boot in comparison to a desktop computer. Simply set a higher timeout than
the default 30 seconds in your project file. Once ready, everything work just
fine.

```clj
{:repl-options {:timeout 120000}}    ;; 2 minutes
```

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


;; declare pin 0 as a digital input with pull resitance set to :down and
;; monitored for edge detection (going from low to high and vice-versa).
(-> 2
    (gpio/mode :input/digital)
    (gpio/pull-resistance :down)
    (gpio/edge-detection :both))
```

## JVM

The 2 common choices are OpenJDK and Oracle. Currently, OpenJDK is not optimized
for ARM and is an order of magnitude slower than Oracle. On the other hand, the
licensing terms for Oracle are somewhat fuzzy.

Hence, we recommend the very promising [Zulu
Embedded](https://www.azul.com/products/zulu-embedded/).

For installation, you will find inspiration
[hhere](https://blog.benjamin-cabe.com/2016/04/05/installing-the-zulu-open-source-java-virtual-machine-on-raspberry-pi).

## Development

We recommend
[sshfs](https://www.digitalocean.com/community/tutorials/how-to-use-sshfs-to-mount-remote-file-systems-over-ssh)
for keeping your files on the Raspberry Pi while being able to edit them on your
dev machine.

Starting the repl in this fashion will make it available on the local network :
```
$ lein repl :start :host 0.0.0.0 :port 4000
```

## Status

In alpha, breaking changes might occur. Other than that, it is already being
used in production.

More capabilities, such as SPI and shift registers, will be added when needed.

While PI4J supports other boards than the Raspberry family, this library has not
been tested for anything else. Because it is lightweight, it should work just
fine.

## License

Copyright © 2017 Adam Helinski

Distributed under the GNU Lesser General Public License.
