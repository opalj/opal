/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * An '''unordered''' set based on the unique ids of the stored [[UID]] objects.
 */
sealed abstract class FrozenUIDSet[+T <: UID] {

    def iterator: Iterator[T]

    def contains(t: UID): Boolean
    def containsId(i: Int): Boolean
}

object EmptyFrozenUIDSet extends FrozenUIDSet[Nothing] {
    def iterator: Iterator[Nothing] = Iterator.empty
    def contains(t: UID): Boolean = false
    def containsId(i: Int): Boolean = false
}

class FrozenUIDArraySet[+T <: UID] private[immutable] (
        private[this] val data: Array[UID]
) extends FrozenUIDSet[T] {

    def iterator: Iterator[T] = data.iterator.asInstanceOf[Iterator[T]] filter (_ != null)
    def contains(t: UID): Boolean = containsId(t.id)
    def containsId(id: Int): Boolean = {
        val length = data.length

        var key = id % length
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

object FrozenUIDSet {

    def empty[T <: UID]: FrozenUIDSet[T] = EmptyFrozenUIDSet

    def apply[T <: UID](es: Iterable[T]): FrozenUIDSet[T] = {
        if (es.isEmpty)
            empty[T]
        else {
            val length = es.size * 2 // this reflects the load factor which is (in this case) 50%
            val data = new Array[UID](length)
            es foreach { e ⇒
                val id = e.id
                var key = id % length
                while (data(key) != null) {
                    key = (key + 1) % length
                }
                data(key) = e
            }
            new FrozenUIDArraySet[T](data)
        }
    }

}
