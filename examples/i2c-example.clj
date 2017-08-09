(ns i2c-example.core
  
  "Configure a DS1631 digital thermometer and read the
   current temperature"

  (:require [pi4clj.i2c :as i2c]))




;;;;;;;;;;


(defn -main

  [& args]

  (if-let [bus (i2c/open-bus "/dev/i2c-1")]
    (do (i2c/select-slave bus
                          0x48)
        (i2c/wr+ bus
                 [0x22               ;; stop slave
                  [0xac 0x0c]        ;; 2 bytes config
                  0x51])             ;; restart slave
        (println "Temperature ="
                 (i2c/rd-reg bus
                              0xaa   ;; register
                              2))    ;; read the temperature as 2 bytes
        (i2c/close-bus bus))
    (println "Can't open the I2C bus!")))
