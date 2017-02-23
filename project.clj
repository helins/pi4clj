(defproject dvlopt/pi4clj
            "0.0.0-alpha0"
  :repl-options {:timeout 120000}
  :description  "IO on the Raspberry Pi"
  :url          "https://github.com/dvlopt/pi4clj"
  :license      {:name "GNU Lesser General Public License version 3.0"
                 :url "https://www.gnu.org/licenses/lgpl.html"}
  :plugins      [[lein-codox "0.9.5"]]
  :codox        {:output-path "doc/auto"
                 :namespaces [pi4clj.gpio
                              pi4clj.i2c]}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [com.pi4j/pi4j-core  "1.1"]
                 ]
  :profiles     {:dev {:source-paths ["dev"]
                       :main         user
                       :plugins      [[venantius/ultra "0.4.1"]                   ;; colorful repl (breaks with clj > 1.9.0-alpha10)
                                      ]
                       :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                      ]}
                 :uberjar {:aot :all}})
