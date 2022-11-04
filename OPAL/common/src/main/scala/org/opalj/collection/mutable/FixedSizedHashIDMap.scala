/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Conceptually, a map where the keys have to have unique hash codes that are spread over a
 * previously known range.
 *
 * Hence, Using this map has three requirements:
 *  1. The value returned by the hashcode function of the keys have to be unique w.r.t. the
 *     values stored in the map; i.e., two different key objects have to have different
 *     hashcode values.
 *  1. The range of hashcode values returned by the keys has to be known and should be reasonably
 *     consecutive because an array will be preallocated to hold all values.
 *     (it can nevertheless start with an arbitrary int)
 *  1. The number of eventually stored key/values should be > 1/4 of the range of key values to
 *     amortize the costs of the underlying data-structures.
 *
 * @note The `null` key is not permitted.
 * @note This data structure is not thread safe.
 *
 * @author Michael Eichberg
 */
class FixedSizedHashIDMap[K <: AnyRef, V] private (
        private var theKeys:        Array[K],
        private var theValues:      Array[V],
        private var hashCodeOffset: Int // basically -minValue
) { self =>

    /**
     * Returns the value stored for the given key.
     *
     * @note If the key is not valid the result is not defined.
     */
    def apply(k: K): V = theValues(k.hashCode() + hashCodeOffset)

    def put(k: K, v: V): this.type = {
        val id = k.hashCode() + hashCodeOffset
        if (id < 0) {
            throw new IllegalArgumentException(s"the key's id (${k.hashCode()}) is < minValue")
        }
        theKeys(id) = k
        theValues(id) = v
        this
    }

    def foreach(f: ((K, V)) => Unit): Unit = {
        val keys = this.theKeys
        val values = this.theValues
        var i = 0
        val max = keys.length
        while (i < max) {
            val k = keys(i)
            // Recall that all values have to be non-null...
            if (k ne null) f((k, values(i)))
            i += 1
        }
    }

    def iterate(f: (K, V) => Unit): Unit = {
        val keys = this.theKeys
        val values = this.theValues
        var i = 0
        val max = keys.length
        while (i < max) {
            val k = keys(i)
            // Recall that all values have to be non-null...
            if (k ne null) f(k, values(i))
            i += 1
        }
    }

    def keys: Iterator[K] = ArraySeq.unsafeWrapArray(theKeys).iterator.filter(key => key != null)

    def entries: Iterator[(K, V)] = new Iterator[(K, V)] {
        private[this] def getNextIndex(lastIndex: Int): Int = {
            val keys = self.theKeys
            val max = keys.length
            var i = lastIndex + 1
            while (i < max) { if (keys(i) ne null) { return i; } else i += 1 }
            max
        }
        private[this] var i = getNextIndex(-1)
        def hasNext: Boolean = i < theKeys.length
        def next(): (K, V) = { val r = (theKeys(i), theValues(i)); i = getNextIndex(i); r }
    }

    def mkString(start: String, sep: String, end: String): String = {
        val keys = this.theKeys
        var s = start
        var i = 0
        val max = keys.length
        while (i < max) {
            val k = keys(i)
            if (k ne null) s += s"$k -> ${theValues(i)}"
            i += 1
            while (i < max && (keys(i) eq null)) i += 1
            if ((k ne null) && i < max) s += sep
        }
        s + end
    }

    override def toString: String = mkString("FixedSizedHashMap(", ", ", ")")

}
object FixedSizedHashIDMap {

    def apply[K <: AnyRef: ClassTag, V: ClassTag](
        minValue: Int, // inclusive
        maxValue: Int // inclusive
    ): FixedSizedHashIDMap[K, V] = {
        if (minValue > maxValue) {
            throw new IllegalArgumentException(s"$minValue > $maxValue");
        }

        val length: Long = maxValue.toLong - minValue.toLong + 1L
        if (length > Int.MaxValue.toLong) {
            throw new IllegalArgumentException(s"range to large: [$minValue,$maxValue]");
        }

        val theKeys: Array[K] = new Array(length.toInt)
        val theValues: Array[V] = new Array(length.toInt)
        val hashCodeOffset: Int = -minValue
        new FixedSizedHashIDMap(theKeys, theValues, hashCodeOffset)
    }

}

