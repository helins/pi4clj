(ns pi4clj.i2c

  "Everything related to I2C.

   Open a bus and simply use the fns.

   <!> Reads and writes are NOT thread-safe !
       Concurrency can be achieved through agents, core.async,
       manifold... Use what you need."

  (:import com.pi4j.jni.I2C))





(defn- -coll->byte-array

  "Transform a collections of integers into an array of bytes
   
   (pi4clj.i2c/-coll->byte-array [0xac 0x0c])"

  [coll]

  (byte-array (map unchecked-byte
                   coll)))




(defn- -byte-array->vec

  "Transform an array of bytes into a collection of 'unsigned' integers"

  [byts]

  (map #(bit-and %
                 0xFF)
       byts))




(defn open-bus

  "Given a path, open a I2C bus. This bus can then be
   used in the fns provided by this namespace.

   Returns nil in case of failure.

   <!> Reads and writes are NOT thread-safe !
       Concurrency can be achieved through agents, core.async,
       manifold... Use whatever you need.

   ex.
     (pi4clj.i2c/open-bus \"/dev/i2c-1\")"

  [bus-path]

  (let [fd (I2C/i2cOpen bus-path)]
    (when-not (neg? fd)
      {:fd   fd
       :path bus-path})))




(defn close-bus

  "Cleanly close an I2C bus previously opened by
   'pi4clj.i2c/open-bus'.

   Should be called when you are done with it.

   Returns true or false whether it suceeded or not."

  [{:keys [fd]
    :as   bus}]

  (zero? (I2C/i2cClose fd)))




(defn wr

  "Given an I2C bus, write something.

   slave-address : an int representing a slave
   byte+ : an int representing a single byte
         | a collection of them

   Returns what has been written, nil in case of failure."

  [{:keys [fd]
    :as   bus} slave-address byte+]

  (when (and fd
             (zero? (if (coll? byte+)
                        (I2C/i2cWriteBytesDirect fd
                                                 slave-address
                                                 (count byte+)
                                                 0
                                                 (-coll->byte-array byte+))
                        (I2C/i2cWriteByteDirect fd
                                                slave-address
                                                (unchecked-byte byte+)))))
    byte+))




(defn wr-reg

  "Same as 'pi4clj.i2c/wr' but writes to a specific register provided
   as an int"

  [{:keys [fd]
    :as   bus} slave-address register byte+]

  (when (and fd
             (zero? (if (coll? byte+)
                        (I2C/i2cWriteBytes fd
                                           slave-address
                                           register
                                           (count byte+)
                                           0
                                           (-coll->byte-array byte+))
                        (I2C/i2cWriteByte fd
                                          slave-address
                                          register
                                          (unchecked-byte byte+)))))
    byte+))




(defn wr+

  "Sometimes, multiple writes have to been done sequentially and
   not at once, for instance when configuring certains slaves.
   
   This fn calls 'pi4clj.i2c/wr' for every element in coll-byte+,
   one at a time, and returns a collection of what has been successfully
   writen."

  [bus slave-address coll-byte+]

  (let [n (reduce (fn [n byte+] (if (wr bus
                                        slave-address
                                        byte+)
                                    (inc n)
                                    (reduced n)))
                  0
                  coll-byte+)]
    (cond (zero? n)
          nil

          (= n
             (count coll-byte+))
          coll-byte+

          ::else
          (take n
                coll-byte+))))




(defn rd

  "Given an I2C bus, read 1 or 'n' bytes from a slave
   adress provided as an int"

  ([{:keys [fd]
     :as   bus} slave-address]
   
   (when-let [byt (and fd
                       (I2C/i2cReadByteDirect fd
                                              slave-address))]
     (when-not (neg? byt)
       byt)))


  ([{:keys [fd]
     :as   bus} slave-address n]

   (when-let [ba (and fd
                      (byte-array n))]
     (when-not (neg? (I2C/i2cReadBytesDirect fd
                                             slave-address
                                             n
                                             0
                                             ba))
       (-byte-array->vec ba)))))




(defn rd-reg

  "Same as 'pi4clj.i2c/rd' but reads from a specific register provided
   as an int"

  ([{:keys [fd]
     :as   bus} slave-address register]

   (when-let [byt (and fd
                       (I2C/i2cReadByte fd
                                        slave-address
                                        register))]
     (when-not (neg? byt)
       byt)))


  ([{:keys [fd]
     :as   bus} slave-address register n]

   (when-let [ba (and fd
                      (byte-array n))]
     (when-not (neg? (I2C/i2cReadBytes fd
                                       slave-address
                                       register
                                       n
                                       0
                                       ba))
       (-byte-array->vec ba)))))
