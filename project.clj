(defproject dvlopt/pi4clj
            "0.0.0-alpha7"

  :description  "IO on the Raspberry Pi"
  :url          "https://github.com/dvlopt/pi4clj"
  :license      {:name "GNU Lesser General Public License version 3.0"
                 :url "https://www.gnu.org/licenses/lgpl.html"}
  :repositories [["sonatype" {:url    "http://oss.sonatype.org/content/groups/public"
                              :update :always}]]
  :dependencies [[com.pi4j/pi4j-core "1.2-SNAPSHOT"]]
  :profiles     {:dev {:source-paths ["dev"
                                      "examples"]
                       :main         user
                       :repl-options {:timeout 120000}
                       :dependencies [[org.clojure/clojure "1.9.0-RC2"]]
                       :plugins      [[venantius/ultra "0.5.1"]
                                      [lein-codox      "0.10.3"]]
                       :codox        {:output-path  "doc/auto"
                                      :source-paths ["src"]
                                      :source-uri  "https://github.com/dvlopt/pi4clj/blob/{version}/{filepath}#L{line}"}
                       :global-vars  {*warn-on-reflection* true}}})
