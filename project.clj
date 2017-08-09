(defproject dvlopt/pi4clj
            "0.0.0-alpha3"

  :description  "IO on the Raspberry Pi"
  :url          "https://github.com/dvlopt/pi4clj"
  :license      {:name "GNU Lesser General Public License version 3.0"
                 :url "https://www.gnu.org/licenses/lgpl.html"}
  :repl-options {:timeout 120000}
  :repositories [["sonatype" {:url    "http://oss.sonatype.org/content/groups/public"
                              :update :always}]]
  :plugins      [[lein-codox "0.10.3"]]
  :codox        {:output-path "doc/auto"
                 :namespaces  [pi4clj.gpio
                               pi4clj.gpio.emulate
                               pi4clj.i2c]
                 :source-uri  "https://github.com/dvlopt/pi4clj/blob/{version}/{filepath}#L{line}"}
  :dependencies [[com.pi4j/pi4j-core "1.2-SNAPSHOT"]]
  :profiles     {:dev {:source-paths ["dev"]
                       :main         user
                       :plugins      [[venantius/ultra "0.5.1"]]
                       :dependencies [[org.clojure/clojure "1.9.0-alpha10"]]
                       :global-vars {*warn-on-reflection* true}}})
