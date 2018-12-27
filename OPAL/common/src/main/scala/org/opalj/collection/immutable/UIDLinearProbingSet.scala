/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * An '''unordered''' set based on the unique ids of the stored [[UID]] objects.
 */
sealed abstract class UIDLinearProbingSet[+T <: UID] {

    def iterator: RefIterator[T]
    def foreach[U](f: T ⇒ U): Unit

    final def contains(t: UID): Boolean = containsId(t.id)
    def containsId(id: Int): Boolean
}

private[immutable] object EmptyUIDLinearProbingSet extends UIDLinearProbingSet[Nothing] {
    override def iterator: RefIterator[Nothing] = RefIterator.empty
    override def foreach[U](f: Nothing ⇒ U): Unit = {}
    override def containsId(id: Int): Boolean = false
}

private[immutable] class UIDLinearProbingSet1[+T <: UID] private[immutable] (
        private[this] val e: T
) extends UIDLinearProbingSet[T] {
    override def iterator: RefIterator[T] = RefIterator(e)
    override def foreach[U](f: T ⇒ U): Unit = f(e)
    override def containsId(id: Int): Boolean = e.id == id
}

private[immutable] class UIDLinearProbingSet2[+T <: UID] private[immutable] (
        private[this] val e1: T,
        private[this] val e2: T
) extends UIDLinearProbingSet[T] {
    override def iterator: RefIterator[T] = RefIterator(e1, e2)
    override def foreach[U](f: T ⇒ U): Unit = { f(e1); f(e2) }
    override def containsId(id: Int): Boolean = e1.id == id || e2.id == id
}

private[immutable] class UIDLinearProbingSet3[+T <: UID] private[immutable] (
        private[this] val e1: T,
        private[this] val e2: T,
        private[this] val e3: T
) extends UIDLinearProbingSet[T] {
    override def iterator: RefIterator[T] = RefIterator(e1, e2, e3)
    override def foreach[U](f: T ⇒ U): Unit = { f(e1); f(e2); f(e3) }
    override def containsId(id: Int): Boolean = e1.id == id || e2.id == id || e3.id == id
}

private[immutable] class UIDArrayLinearProbingSet[+T <: UID] private[immutable] (
        private[this] val data: Array[UID]
) extends UIDLinearProbingSet[T] {

    override def iterator: RefIterator[T] = {
        RefIterator.fromNonNullValues[UID](data).asInstanceOf[RefIterator[T]]
    }

    override def foreach[U](f: T ⇒ U): Unit = {
        val data = this.data
        val max = data.length
        var i = 0
        while (i < max) {
            var d = data(i)
            while (d == null) {
                i += 1
                if (i == max) return ;
                d = data(i)
            }
            i += 1
            f(d.asInstanceOf[T]);
        }
    }

    override def containsId(eid: Int): Boolean = {
        val length = data.length // length is always >= 1
        val id = eid
        var key = id % length
        if (key < 0) key = -key // this is safe, because key will never be Int.MinValue
        var e = data(key)
        if (e == null) {
            false
        } else if (e.id == eid) {
            true
        } else {
            // we use linear probing...
            key = (key + 1) % length
            e = data(key)
            while (e != null) {
                if (e.id == eid) {
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

    /**
     * Creates a new UIDLinearProbingSet.
     *
     * @note UIDSet support a specialized factory method that is more efficient than this one.
     */
    def apply[T <: UID](es: Iterable[T]): UIDLinearProbingSet[T] = {
        es.size match {
            case 0 ⇒ EmptyUIDLinearProbingSet
            case 1 ⇒ new UIDLinearProbingSet1[T](es.head)
            case 2 ⇒
                val it = es.iterator
                new UIDLinearProbingSet2[T](it.next(), it.next())
            case 3 ⇒
                val it = es.iterator
                new UIDLinearProbingSet3[T](it.next(), it.next(), it.next())
            case esSize ⇒
                es.foldLeft(new UIDLinearProbingSetBuilder[T](esSize)) { _ += _ }.result
        }
    }

    def builder[T <: UID](size: Int): UIDLinearProbingSetBuilder[T] = {
        new UIDLinearProbingSetBuilder[T](size)
    }

}

private[immutable] class UIDLinearProbingSetBuilder[T <: UID](size: Int) {

    private[this] val length = size * 2 // reflects the load factor which is (in this case) 50%
    private[this] val data = new Array[UID](length)

    def +=(e: T): this.type = {
        val id = e.id
        var key = id % length
        if (key < 0) key = -key // this is safe, because key will never be Int.MinValue
        while (data(key) != null) {
            key = (key + 1) % length
        }
        data(key) = e
        this
    }

    def result: UIDArrayLinearProbingSet[T] = {
        new UIDArrayLinearProbingSet[T](data)
    }
}
