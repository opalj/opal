/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package mutable

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap

import scala.collection.mutable.ArrayBuffer

/**
 * A thread-safe bidirectional mapping from an object of type `T` to a unique id.
 *
 * The ids must be increasingly ordered before hand and the order will remain when new elements are
 * added.
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
class BidirectionalObject2IDMap[T](
        private[this] var ID2Object: ArrayBuffer[T],
        private[this] val object2ID: Object2IntOpenHashMap[T],
        private[this] var _size:     Int
) {

    def getID(t: T): Int = {
        synchronized {
            object2ID.getInt(t)
        }
    }

    def getData(id: Int): T = {
        synchronized {
            ID2Object(id)
        }
    }

    def addData(t: T): Unit = {
        synchronized {
            object2ID.put(t, _size)
            ID2Object += t
            _size += 1
            assert(_size > 0)
        }
    }

    def this() = {
        this(ArrayBuffer.empty, new Object2IntOpenHashMap(), 0)
    }
}
