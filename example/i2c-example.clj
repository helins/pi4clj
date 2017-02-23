(ns i2c-example.core
  
  "Configure a DS1631 digital thermometer and read the
   current temperature"

  (:require [pi4clj.i2c :as i2c]))




(defn -main

  [& args]

  (if-let [i2c-1 (i2c/open-bus! "/dev/i2c-1")]
    (do (i2c/writes! i2c-1
                     0x48           ;; slave address
                     [0x22          ;; stop slave
                      [0xac 0x0c]   ;; 2 bytes config
                      0x51])        ;; restart slave
        (println "Temperature =" @(i2c/read! 0x48
                                             0xaa
                                             2))   ;; Reads the temperature as 2 bytes from register 0xaa
    (println "Can't open the bus")))
