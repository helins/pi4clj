(ns pi4clj.i2c

  "Everything related to I2C
   
   'open-bus' opens a I2C bus at the given file path.

   The resulting object implements II2CBus and can be used
   for reads and writes to slave devices.

   Don't forget to 'close-bus!' buses that aren't used
   anymore."

  (:import com.pi4j.jni.I2C))




(defprotocol II2CBus

  "Functions for handling I2C buses returned by 'pi4clj.i2c/open-bus!'.
   
   Each bus has its own agent in order to order operations in sequence (most notably
   reads and writes). II2CBus functions typically send a function to the agent and return
   a promise that will be delivered after the sent function is executed."
   

  (close-bus! [this]
    "Close this bus. From now on, reads and writes will be disabled
     and will return nil instead of a promise.
     
     Pending reads and writes will still be executed.

     Returns a promise that will be delivered when the device file is closed.")



  (write! [this slave-address data]
          [this slave-address register data]
    "Write a single byte or a collection of bytes at once to a i2c slave at the register (if given).

     Returns * a promise that will set to * true  if writing succeeded
                                          * false if writing failed
             * nil if the bus is closed
     

     ;; bus defined as 'i2c-1'
     ;; writes 2 bytes at once to slave 0x48 at register 0x10

     @(pi4clj.i2c/write! i2c-1 0x48 0x10 [0xac 0x0c])")



  (writes! [this slave-address data]
           [this slave-address register data]
    "Succesively writes bytes to a i2c slave at the register (if given).
     
     Facilitates such things as configuration and basically any operations requiring
     a sequence of distinct writes.

     Returns * true if all writes succeeded
             * the byte or coll of bytes that failed the sequence
             * nil if the bus is closed
     
     
     ;; bus defined as 'i2c-1'
     ;; configure slave 0x48

     (pi4clj.i2c/writes! i2c-1 0x48 [0x22         ;; stop slave
                                     [0xac 0x0c]  ;; 2 bytes config
                                     0x51])       ;; restart slave")



  (read! [this slave-address n]
         [this slave-address register n]
    "Read 'n' bytes from a i2c slave at the register (if given).

     Returns * a promise that will be set to * resulting coll of bytes if reading succeeded
                                             * nil                     if reading failed
             * nil if the bus is closed

     ;; bus defined as 'i2c-1'
     ;; reads 2 bytes from slave 0x48 at register 0xaa

     @(pi4clj.i2c/read! i2c-1 0x48 0xaa 2)"))




(defn- coll->bytes

  "Transform a collections of integers into an array of bytes
   
   (pi4clj.i2c/coll->bytes [0xac 0x0c])"

  [coll]

  (byte-array (map unchecked-byte
                   coll)))




(defn- bytes->coll

  "Transform an array of bytes into a collection of 'unsigned' integers"

  [byts]

  (map #(bit-and % 0xFF)
       byts))




(defn open-bus!

  "Open a I2C bus at the given file path.
   
   Returns * an object implementing II2CBus if opening succeeded
           * nil                            if opening failed


   (def i2c-1 (pi4clj.i2c/open-bus! \"/dev/i2c-1\"))"

  [bus-path]

  (let [fd (I2C/i2cOpen bus-path)]
    (when (>= fd 0)
      (let [open? (atom true)
            queue (agent nil
                         :error-mode :continue)
            send-to-queue (fn [force? f]
                            (when (or force?
                                      @open?)
                              (let [p (promise)]
                                (send-off queue
                                          (fn [_] (deliver p (f))))
                                p)))]
    (reify II2CBus

      (close-bus! [_]
        (reset! open?
                false)
        (send-to-queue true
                       (fn []
                         (I2C/i2cClose fd)
                         true)))



      (write! [_ slave-address data]
        (send-to-queue false
                       (if (coll? data)
                           (let [data' (coll->bytes data)]
                             #(= (I2C/i2cWriteBytesDirect fd
                                                          slave-address
                                                          (count data')
                                                          0
                                                          data')
                                 0))
                           #(= (I2C/i2cWriteByteDirect fd
                                                       slave-address
                                                       (unchecked-byte data))
                               0))))



      (write! [_ slave-address register data]
        (send-to-queue false
                       (if (coll? data)
                           (let [data' (coll->bytes data)]
                             #(= (I2C/i2cWriteBytes fd
                                                    slave-address
                                                    register
                                                    (count data')
                                                    0
                                                    data')
                                 0))
                           #(= (I2C/i2cWriteByte fd
                                                 slave-address
                                                 register
                                                 (unchecked-byte data))
                               0))))



      (writes! [this slave-address data]
        (loop [data' data]
          (when data'
            (if @(write! this
                         slave-address
                         (first data'))
                (recur (next data'))
                (first data')))))



      (writes! [this slave-address register data]
        (loop [data' data]
          (when data'
            (if @(write! this
                         slave-address
                         register
                         (first data'))
                (recur (next data'))
                (first data')))))



      (read! [_ slave-address n]
        (send-to-queue false
                       #(let [arr (make-array Byte/TYPE n)]
                          (when (>= (I2C/i2cReadBytesDirect fd
                                                            slave-address
                                                            n
                                                            0
                                                            arr)
                                    0)
                            (bytes->coll arr)))))



      (read! [_ slave-address register n]
        (send-to-queue false
                       #(let [arr (make-array Byte/TYPE n)]
                          (when (>= (I2C/i2cReadBytes fd
                                                      slave-address
                                                      register
                                                      n
                                                      0
                                                      arr)
                                    0)
                            (bytes->coll arr))))))))))
