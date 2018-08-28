/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * An '''unordered''' set based on the unique ids of the stored [[UID]] objects.
 */
sealed abstract class UIDLinearProbingSet[+T <: UID] {

    def iterator: Iterator[T]

    def contains(t: UID): Boolean
    def containsId(i: Int): Boolean
}

private[immutable] object EmptyUIDLinearProbingSet extends UIDLinearProbingSet[Nothing] {
    def iterator: Iterator[Nothing] = Iterator.empty
    def contains(t: UID): Boolean = false
    def containsId(i: Int): Boolean = false
}

private[immutable] class UIDArrayLinearProbingSet[+T <: UID] private[immutable] (
        private[this] val data: Array[UID]
) extends UIDLinearProbingSet[T] {

    def iterator: AnyRefIterator[T] = {
        AnyRefIterator.fromNonNullValues[UID](data).asInstanceOf[AnyRefIterator[T]]
    }

    def contains(t: UID): Boolean = containsId(t.id)
    def containsId(id: Int): Boolean = {
        val length = data.length // length is always >= 1

        var key = id % length
        if (key < 0) key = -key // this is safe, because key will never be Int.MinValue
        var e = data(key)
        if (e == null) {
            false
        } else if (e.id == id) {
            true
        } else {
            // we use linear probing...
            key = (key + 1) % length
            e = data(key)
            while (e != null) {
                if (e.id == id) {
                    return true;
                }
                key = (key + 1) % length
                e = data(key)
            }
            false
        }
    }
}

object UIDLinearProbingSet {

    def empty[T <: UID]: UIDLinearProbingSet[T] = EmptyUIDLinearProbingSet

    def apply[T <: UID](es: Iterable[T]): UIDLinearProbingSet[T] = {
        if (es.isEmpty)
            empty[T]
        else {
            val length = es.size * 2 // this reflects the load factor which is (in this case) 50%
            val data = new Array[UID](length)
            es foreach { e ⇒
                val id = e.id
                var key = id % length
                if (key < 0) key = -key // this is safe, because key will never be Int.MinValue
                while (data(key) != null) {
                    key = (key + 1) % length
                }
                data(key) = e
            }
            new UIDArrayLinearProbingSet[T](data)
        }
    }

}
