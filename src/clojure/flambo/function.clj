(ns flambo.function
  (:require [serializable.fn :as sfn]
            [flambo.kryo :as kryo]
            [flambo.cache :as cache])
  (:import [org.apache.spark.api.java.function
            Function
            Function2
            Function3
            VoidFunction
            VoidFunction2
            FlatMapFunction
            FlatMapFunction2
            ForeachPartitionFunction
            PairFlatMapFunction
            PairFunction
            DoubleFunction
            DoubleFlatMapFunction
            MapFunction
            ReduceFunction
            FilterFunction
            MapPartitionsFunction
            MapGroupsFunction
            FlatMapGroupsFunction]
           [org.apache.spark.sql.api.java
            UDF1
            UDF2
            UDF3
            UDF4
            UDF5
            UDF6
            UDF7
            UDF8
            UDF9
            UDF10
            UDF11
            UDF12
            UDF13
            UDF14
            UDF15
            UDF16
            UDF17
            UDF18
            UDF19
            UDF20
            UDF21
            UDF22]))

(defn- serfn? [f]
  (= (type f) :serializable.fn/serializable-fn))

(def serialize-fn sfn/serialize)


;; XXX: memoizing here is weird because all functions in a JVM now share a single
;;      cache lookup. Maybe we could memoize in the constructor or something instead?
;; TODO: what is a good cache size here???
(def deserialize-fn (cache/lru-memoize 5000 sfn/deserialize))
(def array-of-bytes-type (Class/forName "[B"))

;; ## Generic
(defn -init
  "Save the function f in state"
  [f]
  [[] f])

;; ## Functions
(defn mk-sym
  [fmt sym-name]
  (symbol (format fmt sym-name)))

(defn check-not-nil! [x message]
  (when (nil? x) (throw (ex-info message {}))))

(defmacro gen-function
  [clazz wrapper-name]
  (let [new-class-sym (mk-sym "flambo.function.%s" clazz)
        prefix-sym (mk-sym "%s-" clazz)]
    `(do
       (gen-class
        :name ~new-class-sym
        :extends flambo.function.AbstractFlamboFunction
        :implements [~(mk-sym (if (re-find #"^UDF" (name clazz)) "org.apache.spark.sql.api.java.%s" "org.apache.spark.api.java.function.%s") clazz)]
        :prefix ~prefix-sym
        :init ~'init
        :state ~'state
        :constructors {[Object] []})
       (def ~(mk-sym "%s-init" clazz) -init)
       (defn ~(mk-sym "%s-call" clazz)
         [~(vary-meta 'this assoc :tag new-class-sym) & ~'xs]
         (check-not-nil! ~'this "Nil this func instance")
         (check-not-nil! ~'xs "Nil xs args")
         (let [fn-or-serfn# (.state ~'this)
               _# (check-not-nil! fn-or-serfn# "Nil fn-or-serfn state")
               f# (if (instance? array-of-bytes-type fn-or-serfn#)
                    (binding [sfn/*deserialize* kryo/deserialize]
                      (deserialize-fn fn-or-serfn#))
                    fn-or-serfn#)]
           (check-not-nil! f# "Nil func or serialize func")
           (apply f# ~'xs)))
       (defn ~(vary-meta wrapper-name assoc :tag clazz)
         [f#]
         (check-not-nil! f# "Nil func or serialize func wrapper")
         (new ~new-class-sym
              (if (serfn? f#)
                (binding [sfn/*serialize* kryo/serialize]
                  (serialize-fn f#)) f#))))))

(gen-function Function function)
(gen-function Function2 function2)
(gen-function Function3 function3)
(gen-function VoidFunction void-function)
(gen-function VoidFunction2 void-function2)
(gen-function FlatMapFunction flat-map-function)
(gen-function FlatMapFunction2 flat-map-function2)
(gen-function PairFlatMapFunction pair-flat-map-function)
(gen-function PairFunction pair-function)
(gen-function DoubleFunction double-function)
(gen-function DoubleFlatMapFunction double-flat-map-function)
(gen-function MapFunction map-function)
(gen-function ReduceFunction reduce-function)
(gen-function FilterFunction filter-function)
(gen-function ForeachPartitionFunction for-each-partition-function)
(gen-function MapPartitionsFunction map-partitions-function)
(gen-function MapGroupsFunction map-groups-function)
(gen-function FlatMapGroupsFunction flat-map-groups-function)
(gen-function UDF1 udf)
(gen-function UDF2 udf2)
(gen-function UDF3 udf3)
(gen-function UDF4 udf4)
(gen-function UDF5 udf5)
(gen-function UDF6 udf6)
(gen-function UDF7 udf7)
(gen-function UDF8 udf8)
(gen-function UDF9 udf9)
(gen-function UDF10 udf10)
(gen-function UDF11 udf11)
(gen-function UDF12 udf12)
(gen-function UDF13 udf13)
(gen-function UDF14 udf14)
(gen-function UDF15 udf15)
(gen-function UDF16 udf16)
(gen-function UDF17 udf17)
(gen-function UDF18 udf18)
(gen-function UDF19 udf19)
(gen-function UDF20 udf20)
(gen-function UDF21 udf21)
(gen-function UDF22 udf22)

