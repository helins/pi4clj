# pi4clj

![Raspberry Pi](http://i.imgur.com/FeW5Q.png)

Handle GPIOs and I2C (and more to come) on the Raspberry Pi and similar boards.

This library relies on [PI4J](http://www.pi4j.com), more specifically the native bindings to
[Gordon Henderson's wiringPi](http://www.wiringpi.com). It aims to be straightforward and not
very opiniated. No black magic involved.

## Installation

No prior installation is required, simply add this to your dependencies :
```clj
[dvlopt/pi4clj "0.0.0-alpha2"]
```

Interactive development on the board itself is a bliss. Nonetheless, lein's repl can be very slow
to start and might even timeout. Simply set a higher timeout than the default 30s in your project file :
```clj
{:repl-options {:timeout 120000}}    ;; 2 minutes
```

OpenJDK isn't optimized for ARM at all and is an order of magnitude slower than Oracle's JDK. On
the other hand, Oracle's JDK... well, in one word, "licensing". That's why we recommend the very
promising [Zulu Embedded](https://www.azul.com/products/zulu-embedded/) for running java and clojure
on the raspberry pi. For installation, go [there](https://blog.benjamin-cabe.com/2016/04/05/installing-the-zulu-open-source-java-virtual-machine-on-raspberry-pi).

## Usage

Small examples are provided in the 'examples' folder.

```clj
(require '[pi4clj.gpio :as gpio])

(gpio/listen :sync
             (fn [pin state]
               (when (= pin 2)
                 (gpio/wr-digital 0
                                  state))))
(gpio/scheme :wiring-pi)
(gpio/digital-out 0
                  false)
(gpio/digital-in 2
                 {:pull     :down
                  :monitor? true}))
```

## Documentation

You will find extensive descriptions of everything you need in the latest
[api documentation](https://dvlopt.github.io/doc/pi4clj).

If you are browsing an older version, try generating the auto-doc yourself
```
cd your_project/dir
lein codox
cd doc/codox
```

## Status

In alpha, breaking changes are very much expected for the time being.

More capabilities, such as SPI and shift registers, will be added.

While PI4J supports other boards than the Raspberry family, this library hasn't been tested for
anything else. Because it is lightweight, it should work just fine.

## License

Copyright © 2017 Adam Helinski

Distributed under the GNU Lesser General Public License.
