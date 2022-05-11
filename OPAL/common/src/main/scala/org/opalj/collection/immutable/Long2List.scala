/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import java.lang.{Long => JLong}

/**
 * A growable, immutable linked list based data store for '''long values'''.
 * Though this data store is backed by a linked list, it is not a classical linked list and it
 * does not offer (efficient) head and tail operations. However, compared to a classical linked
 * list the memory usage is reduced significantly and a simple foreach is also faster.
 *
 * This list does not perform any length related checks. I.e., it fails,e.g., in
 * case of `forFirstN` if the size of the list is smaller than expected.
 *
 * Furthermore, all directly implemented methods use `while` loops for maximum
 *
 * @author Michael Eichberg
 */
sealed abstract class Long2List extends Serializable { self =>

    def isEmpty: Boolean
    def isSingletonList: Boolean
    def nonEmpty: Boolean

    def head: Long
    def foreach[U](f: Long => U): Unit
    def forall(p: Long => Boolean): Boolean
    def foldLeft[B](z: B)(op: (B, Long) => B): B
    /** Iterates over the first N values. */
    def forFirstN[U](n: Int)(f: Long => U): Unit

    /**
     * @return An iterator over this list's values. In general, you should prefer using foreach or
     *         the other specialized methods to avoid the overhead incurred by the generation of
     *         the iterator.
     */
    def iterator: LongIterator

    /** Prepends the given value to this list. E.g., `l = 2L +: l`. */
    def +:(v: Long): Long2List

    override def equals(other: Any): Boolean
    override def hashCode(): Int // just added to ensure that we have to override hashCode!

    final override def toString = iterator.mkString("Long2List(", ", ", ")")
}

object Long2List {

    def empty: Long2List = Long2List0

    def apply(v: Long): Long2List = new Long2List1(v, null)

    def apply(v1: Long, v2: Long): Long2List = new Long2List2(v1, v2, null)

    def apply(v1: Long, v2: Long, v3: Long): Long2List = new Long2List3(v1, v2, v3, null)

    def apply(v1: Long, v2: Long, v3: Long, v4: Long): Long2List = {
        new Long2List4(v1, v2, v3, v4, null)
    }

}

/**
 * An empty [[Long2List]].
 *
 * @author Michael Eichberg
 */
case object Long2List0 extends Long2List {

    override def isEmpty: Boolean = true
    override def isSingletonList: Boolean = false
    override def nonEmpty: Boolean = false

    override def head: Long = throw new UnsupportedOperationException
    override def foreach[U](f: Long => U): Unit = {}
    override def forall(p: Long => Boolean): Boolean = true
    override def foldLeft[B](z: B)(op: (B, Long) => B): B = z
    /** Iterates over the first N values. */
    override def forFirstN[U](n: Int)(f: Long => U): Unit = {}
    override def iterator: LongIterator = LongIterator.empty
    /** Prepends the given value to this list. E.g., `l = 2l +: l`. */
    override def +:(v: Long): Long2List = new Long2List1(v, null)

    override def equals(other: Any): Boolean = {
        other match { case that: AnyRef => this eq that; case _ => false }
    }
    override def hashCode(): Int = 37
}

private[immutable] abstract class Long2List1_4 extends Long2List {

    final override def isEmpty: Boolean = false
    final override def nonEmpty: Boolean = true

    private[immutable] def rest: Long2List4

    final def restForeach[U](f: Long => U): Unit = {
        var list: Long2List4 = this.rest
        while (list != null) {
            f(list.v1)
            f(list.v2)
            f(list.v3)
            f(list.v4)
            list = list.rest
        }
    }

    final def restFoldLeft[B](z: B)(op: (B, Long) => B): B = {
        var r = z
        var list: Long2List4 = this.rest
        while (list != null) {
            r = op(op(op(op(r, list.v1), list.v2), list.v3), list.v4)
            list = list.rest
        }
        r
    }

    final def restForall(p: Long => Boolean): Boolean = {
        var list: Long2List4 = this.rest
        while (list != null) {
            if (!(p(list.v1) && p(list.v2) && p(list.v3) && p(list.v4)))
                return false;
            list = list.rest
        }
        true
    }

    final def restForFirstN[U](n: Int)(f: Long => U): Unit = {
        var i = n
        var list: Long2List4 = rest
        while (i > 0) {
            i match {
                case 1 =>
                    f(list.v1); i = 0
                case 2 =>
                    f(list.v1); f(list.v2); i = 0
                case 3 =>
                    f(list.v1); f(list.v2); f(list.v3); i = 0
                case _ =>
                    f(list.v1); f(list.v2); f(list.v3); f(list.v4)
                    i -= 4
                    if (i > 0) {
                        list = list.rest
                    }
            }
        }
    }

    final def restIterator(): LongIterator = {
        new LongIterator {
            private[this] var list: Long2List4 = rest
            private[this] var index: Int = 0
            def hasNext: Boolean = list != null
            def next(): Long = {
                index match {
                    case 0 => { index += 1; list.v1 }
                    case 1 => { index += 1; list.v2 }
                    case 2 => { index += 1; list.v3 }
                    case 3 => { index = 0; val v = list.v4; list = list.rest; v }
                }
            }
        }
    }

    // ... this node is also a Long2List4 node
    final def restEquals(otherRest: Long2List4): Boolean = {
        var thisRest = this.rest
        var thatRest = otherRest
        while (thisRest != null && thatRest != null) {
            val equal = thisRest.v1 == thatRest.v1 &&
                thisRest.v2 == thatRest.v2 &&
                thisRest.v3 == thatRest.v3 &&
                thisRest.v4 == thatRest.v4
            if (!equal)
                return false;
            thisRest = thisRest.rest
            thatRest = thatRest.rest
        }
        thatRest == null && thisRest == null
    }

    final def restHashCode(): Int = {
        var h = 31
        var list: Long2List4 = rest
        while (list != null) {
            h += JLong.hashCode(list.v1) * 31
            h += JLong.hashCode(list.v2) * 31
            h += JLong.hashCode(list.v3) * 31
            h += JLong.hashCode(list.v4) * 31
            list = list.rest
        }
        h
    }
}

private[immutable] final class Long2List1(
        private[immutable] val v1:   Long,
        private[immutable] val rest: Long2List4
) extends Long2List1_4 { list =>

    override def isSingletonList: Boolean = rest == null

    override def head: Long = v1

    override def foreach[U](f: Long => U): Unit = {
        f(v1);
        restForeach(f)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        restFoldLeft(op(z, v1))(op)
    }

    override def forall(p: Long => Boolean): Boolean = {
        p(v1) && restForall(p)
    }

    override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n > 0) {
            f(v1)
            restForFirstN(n - 1)(f)
        }
    }

    override def iterator: LongIterator = {
        var it = LongIterator(v1)
        if (rest != null) it ++= restIterator()
        it
    }

    override def +:(v: Long): Long2List = new Long2List2(v, v1, rest)

    override def equals(other: Any): Boolean = {
        other match {
            case that: Long2List1 =>
                (that eq this) || (
                    that.v1 == this.v1 && restEquals(that.rest)
                )
            case _ => false
        }
    }

    override def hashCode(): Int = JLong.hashCode(v1) * 31 + restHashCode()

}

private[immutable] final class Long2List2(
        private[immutable] var v1:   Long,
        private[immutable] var v2:   Long,
        private[immutable] var rest: Long2List4
) extends Long2List1_4 { list =>

    override def isSingletonList: Boolean = false

    override def head: Long = v1

    override def foreach[U](f: Long => U): Unit = { f(v1); f(v2); restForeach(f) }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = restFoldLeft(op(op(z, v1), v2))(op)

    override def forall(p: Long => Boolean): Boolean = p(v1) && p(v2) && restForall(p)

    override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n > 0) {
            f(v1)
            if (n > 1) {
                f(v2)
                if (n > 2) {
                    restForFirstN(n - 2)(f)
                }
            }
        }
    }

    override def iterator: LongIterator = {
        var it = LongIterator(v1, v2)
        if (rest != null) it ++= restIterator()
        it
    }

    override def +:(v: Long): Long2List = new Long2List3(v, v1, v2, rest)

    override def equals(other: Any): Boolean = {
        other match {
            case that: Long2List2 =>
                (that eq this) || (
                    that.v1 == this.v1 && that.v2 == this.v2 && restEquals(that.rest)
                )
            case _ => false
        }
    }

    override def hashCode(): Int = JLong.hashCode(v1) * 31 + JLong.hashCode(v2) * 31 + restHashCode()
}

private[immutable] final class Long2List3(
        private[immutable] var v1:   Long,
        private[immutable] var v2:   Long,
        private[immutable] var v3:   Long,
        private[immutable] var rest: Long2List4
) extends Long2List1_4 { list =>

    override def isSingletonList: Boolean = false

    override def head: Long = v1

    override def foreach[U](f: Long => U): Unit = { f(v1); f(v2); f(v3); restForeach(f) }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        restFoldLeft(op(op(op(z, v1), v2), v3))(op)
    }

    override def forall(p: Long => Boolean): Boolean = p(v1) && p(v2) && p(v3) && restForall(p)

    override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n > 0) {
            f(v1)
            if (n > 1) {
                f(v2)
                if (n > 2) {
                    f(v3)
                    if (n > 3) {
                        restForFirstN(n - 3)(f)

                    }
                }
            }
        }
    }

    override def iterator: LongIterator = {
        var it = LongIterator(v1, v2, v3)
        if (rest != null) it ++= restIterator()
        it
    }

    override def +:(v: Long): Long2List = new Long2List4(v, v1, v2, v3, rest)

    override def equals(other: Any): Boolean = {
        other match {
            case that: Long2List3 =>
                (that eq this) || (
                    that.v1 == v1 && that.v2 == v2 && that.v3 == v3 && restEquals(that.rest)
                )
            case _ => false
        }
    }

    override def hashCode(): Int = {
        JLong.hashCode(v1) * 31 + JLong.hashCode(v2) * 31 + JLong.hashCode(v3) * 31 + restHashCode()
    }
}

private[immutable] final class Long2List4(
        private[immutable] var v1:   Long,
        private[immutable] var v2:   Long,
        private[immutable] var v3:   Long,
        private[immutable] var v4:   Long,
        private[immutable] var rest: Long2List4
) extends Long2List1_4 { list =>

    override def isSingletonList: Boolean = false

    override def head: Long = v1

    override def foreach[U](f: Long => U): Unit = {
        f(v1); f(v2); f(v3); f(v4); restForeach(f)
    }

    override def foldLeft[B](z: B)(op: (B, Long) => B): B = {
        restFoldLeft(op(op(op(op(z, v1), v2), v3), v4))(op)
    }

    override def forall(p: Long => Boolean): Boolean = {
        p(v1) && p(v2) && p(v3) && p(v4) && restForall(p)
    }

    override def forFirstN[U](n: Int)(f: Long => U): Unit = {
        if (n > 0) {
            f(v1)
            if (n > 1) {
                f(v2)
                if (n > 2) {
                    f(v3)
                    if (n > 3) {
                        f(v4)
                        if (n > 4) {
                            restForFirstN(n - 4)(f)
                        }
                    }
                }
            }
        }
    }

    override def iterator: LongIterator = {
        var it = LongIterator(v1, v2, v3, v4)
        if (rest != null) it ++= restIterator()
        it
    }

    override def +:(v: Long): Long2List = new Long2List1(v, this)

    override def equals(other: Any): Boolean = {
        other match {
            case that: Long2List4 =>
                (that eq this) || (
                    that.v1 == v1 && that.v2 == v2 && that.v3 == v3 && that.v4 == v4 &&
                    restEquals(that.rest)
                )
            case _ => false
        }
    }

    override def hashCode(): Int = {
        JLong.hashCode(v1) * 31 +
            JLong.hashCode(v2) * 31 +
            JLong.hashCode(v3) * 31 +
            JLong.hashCode(v4) * 31 +
            restHashCode()
    }
}