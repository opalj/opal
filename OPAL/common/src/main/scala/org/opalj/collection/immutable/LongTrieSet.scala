/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.collection
package immutable

import java.lang.{Long ⇒ JLong}
import java.util.function.LongConsumer
import scala.collection.AbstractIterator

/**
 * An unordered set of long values backed by a trie set. The branching is done using
 * the least significant bit and values are only stored in leaf nodes. This ensure that
 * we have a stable iteration order.
 *
 * @author Michael Eichberg
 */
sealed abstract class LongTrieSet
    extends LongSet[LongTrieSet]
    with LongCollectionWithStableOrdering[LongTrieSet]
    with LongWorkSet[LongTrieSet] { longSet ⇒

    /**
     * Returns each pairing of two values. I.e., if the set contains 1, 4, 8, the pairings
     * ((1,4) XOR (4,1)),((1,8) XOR (8,1)) and ((4,8) XOR (8,4)) will be returned; hence,
     * the order between the two values is not defined.
     */
    def foreachPair[U](f: (Long, Long) ⇒ U): Unit
    def intersect(other: LongTrieSet): LongTrieSet = {
        if (other.size <= 2)
            // we have specialized handling for small sets
            return other.intersect(this);

        val (smallerSet, largerSet) = if (other.size > this.size) (this, other) else (other, this)
        var r = smallerSet
        val it = smallerSet.longIterator
        while (it.hasNext) {
            val n = it.next
            if (!largerSet.contains(n)) {
                r -= n
            }
        }
        r
    }

    /** Returns some value and removes it from this set. */
    def getAndRemove: LongHeadAndRestOfSet[LongTrieSet]

    def filter(p: Long ⇒ Boolean): LongTrieSet
    override def withFilter(p: Long ⇒ Boolean): LongTrieSet

    final override def subsetOf(other: LongTrieSet): Boolean = subsetOf(other, 0)

    final override def toString: String = mkString("LongTrieSet(", ",", ")")

    /**
     * Tries to add the given method to this trie set by ''mutating the set if possible''.
     * Due to the longernal organization, mutating the set is not always possible. In this case, a
     * new set containing the new value is returned. Hence, the return value ''must not'' be
     * ignored!
     */
    def +!(value: Long): LongTrieSet

    /**
     * @see `+!(Long)` for details!
     */
    final def ++!(that: LongTrieSet): LongTrieSet = {
        that.foldLeft(this)(_ +! _) // We have to expand `this`!
    }
    //
    // IMPLEMENTATION "INTERNAL" METHODS
    //

    private[immutable] def +(i: Long, level: Long): LongTrieSet
    private[immutable] def +!(i: Long, level: Long): LongTrieSet
    private[immutable] def -(i: Long, key: Long): LongTrieSet
    private[immutable] def contains(value: Long, key: Long): Boolean
    private[immutable] def subsetOf(other: LongTrieSet, level: Long): Boolean

    /** Ensures that this set is represented using its canonical representation. */
    private[immutable] def constringe(): LongTrieSet
}

final class FilteredLongTrieSet(
        private val s: LongTrieSet,
        private val p: Long ⇒ Boolean
) extends LongTrieSet {

    override def iterator: Iterator[Long] = s.iterator.withFilter(p)
    override def longIterator: LongIterator = s.longIterator.withFilter(p)

    override def foreach(f: LongConsumer): Unit = s.foreach { i ⇒ if (p(i)) f.accept(i) }
    override def map(f: Long ⇒ Long): LongTrieSet = {
        s.foldLeft(EmptyLongTrieSet: LongTrieSet) { (c, i) ⇒ if (p(i)) c +! f(i) else c }
    }
    override def flatMap(f: Long ⇒ LongTrieSet): LongTrieSet = {
        s.flatMap(i ⇒ if (p(i)) f(i) else EmptyLongTrieSet)
    }
    override def withFilter(p: (Long) ⇒ Boolean): LongTrieSet = {
        new FilteredLongTrieSet(s, i ⇒ p(i) && this.p(i))
    }

    override def exists(p: Long ⇒ Boolean): Boolean = s.exists(i ⇒ this.p(i) && p(i))
    override def forall(f: Long ⇒ Boolean): Boolean = s.forall(i ⇒ !this.p(i) || f(i))
    override def contains(value: Long): Boolean = p(value) && s.contains(value)

    private[this] lazy val filtered: LongTrieSet = s.filter(p)

    override def intersect(other: LongTrieSet): LongTrieSet = filtered.intersect(other)
    override def foreachPair[U](f: (Long, Long) ⇒ U): Unit = filtered.foreachPair(f)
    override def filter(p: Long ⇒ Boolean): LongTrieSet = filtered.filter(p)
    override def isSingletonSet: Boolean = filtered.isSingletonSet
    override def hasMultipleElements: Boolean = filtered.hasMultipleElements
    override def isEmpty: Boolean = filtered.isEmpty
    override def size: Int = filtered.size
    override def head: Long = filtered.head
    override def getAndRemove: LongHeadAndRestOfSet[LongTrieSet] = filtered.getAndRemove
    override def -(i: Long): LongTrieSet = filtered - i
    override def +(i: Long): LongTrieSet = filtered + i
    override def +!(value: Long): LongTrieSet = filtered +! value
    override def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B = filtered.foldLeft(z)(f)
    override def equals(other: Any): Boolean = filtered.equals(other)
    override def hashCode: Int = filtered.hashCode()

    // Actually the following methods should never be called... in a sense they are dead.
    private[immutable] override def constringe(): LongTrieSet = filtered.constringe()
    private[immutable] override def -(i: Long, key: Long): LongTrieSet = filtered - (i, key)
    private[immutable] override def +(i: Long, level: Long): LongTrieSet = filtered + (i, level)
    private[immutable] override def +!(i: Long, level: Long): LongTrieSet = filtered +! (i, level)
    private[immutable] override def contains(value: Long, key: Long): Boolean = {
        filtered.contains(value, key)
    }
    private[immutable] override def subsetOf(other: LongTrieSet, level: Long): Boolean = {
        filtered.subsetOf(other, level)
    }
}

/** The (potential) leaves of an LongTrie. */
private[immutable] sealed abstract class LongTrieSetL extends LongTrieSet {

    final override private[immutable] def -(i: Long, key: Long): LongTrieSet = this.-(i)
    final override private[immutable] def constringe(): LongTrieSet = this
    final override private[immutable] def contains(value: Long, key: Long): Boolean = {
        this.contains(value)
    }
}

case object EmptyLongTrieSet extends LongTrieSetL {
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = false
    override def isEmpty: Boolean = true
    override def size: Int = 0
    override def head: Long = throw new UnsupportedOperationException("empty")
    override def getAndRemove: LongHeadAndRestOfSet[LongTrieSet] = {
        throw new UnsupportedOperationException("empty")
    }
    override def foreach(f: LongConsumer): Unit = {}
    override def foreachPair[U](f: (Long, Long) ⇒ U): Unit = {}
    override def filter(p: (Long) ⇒ Boolean): LongTrieSet = this
    override def withFilter(p: (Long) ⇒ Boolean): LongTrieSet = this
    override def map(f: Long ⇒ Long): LongTrieSet = this
    override def -(i: Long): this.type = this
    override def +(i: Long): LongTrieSet1 = LongTrieSet1(i)
    override def +!(i: Long): LongTrieSet = LongTrieSet1(i)
    override def intersect(other: LongTrieSet): LongTrieSet = this
    override def iterator: Iterator[Long] = Iterator.empty
    override def longIterator: LongIterator = LongIterator.empty
    override def contains(value: Long): Boolean = false
    override def exists(p: Long ⇒ Boolean): Boolean = false
    override def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B = z
    override def forall(f: Long ⇒ Boolean): Boolean = true
    override def flatMap(f: Long ⇒ LongTrieSet): LongTrieSet = this

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSet ⇒ that.isEmpty
            case _                 ⇒ false
        }
    }

    override def hashCode: Int = 0 // compatible to Arrays.hashCode

    private[immutable] override def +(i: Long, level: Long): LongTrieSet = this.+(i)
    private[immutable] override def +!(i: Long, level: Long): LongTrieSet = this.+!(i)
    private[immutable] override def subsetOf(other: LongTrieSet, level: Long): Boolean = true
}

final case class LongTrieSet1 private (i: Long) extends LongTrieSetL {
    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = true
    override def hasMultipleElements: Boolean = false
    override def size: Int = 1
    override def foreach(f: java.util.function.LongConsumer): Unit = { f.accept(i) }
    override def foreachPair[U](f: (Long, Long) ⇒ U): Unit = {}
    override def getAndRemove: LongHeadAndRestOfSet[LongTrieSet] = {
        LongHeadAndRestOfSet(i, EmptyLongTrieSet: LongTrieSet)
    }
    override def filter(p: Long ⇒ Boolean): LongTrieSet = if (p(i)) this else EmptyLongTrieSet
    override def withFilter(p: Long ⇒ Boolean): LongTrieSet = new FilteredLongTrieSet(this, p)
    override def map(f: Long ⇒ Long): LongTrieSet = {
        val newI = f(i)
        if (newI != i)
            LongTrieSet1(newI)
        else
            this
    }
    override def flatMap(f: Long ⇒ LongTrieSet): LongTrieSet = f(i)
    override def head: Long = i
    override def -(i: Long): LongTrieSet = if (this.i != i) this else EmptyLongTrieSet
    override def +(i: Long): LongTrieSet = if (this.i == i) this else LongTrieSet.from(this.i, i)
    override def +!(i: Long): LongTrieSet = this + i
    override def iterator: Iterator[Long] = Iterator.single(i)
    override def longIterator: LongIterator = LongIterator(i)
    override def intersect(other: LongTrieSet): LongTrieSet = {
        if (other.contains(this.i)) this else EmptyLongTrieSet
    }
    override def contains(value: Long): Boolean = value == i
    override def exists(p: Long ⇒ Boolean): Boolean = p(i)
    override def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B = f(z, i)
    override def forall(f: Long ⇒ Boolean): Boolean = f(i)

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSet ⇒ that.isSingletonSet && this.i == that.head
            case _                 ⇒ false
        }
    }

    override def hashCode: Int = 31 + JLong.hashCode(i) // compatible to Arrays.hashCode

    override private[immutable] def +(i: Long, level: Long): LongTrieSet = this.+(i)
    override private[immutable] def +!(i: Long, level: Long): LongTrieSet = this.+!(i)
    override private[immutable] def subsetOf(other: LongTrieSet, level: Long): Boolean = {
        other.contains(i, i >>> level)
    }
}

object LongTrieSet1 {

    def apply(v: Long) = new LongTrieSet1(v)

}

/**
 * Represents an ordered set of two values where i1 has to be smaller than i2.
 */
private[immutable] final class LongTrieSet2 private[immutable] (
        val i1: Long, val i2: Long
) extends LongTrieSetL {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def size: Int = 2
    override def head: Long = i2
    override def getAndRemove: LongHeadAndRestOfSet[LongTrieSet] = {
        LongHeadAndRestOfSet[LongTrieSet](i2, LongTrieSet1(i1))
    }

    override def iterator: Iterator[Long] = new AbstractIterator[Long] {
        private[this] var i = 0
        def hasNext: Boolean = i < 2
        def next: Long = if (i == 0) { i = 1; i1 } else { i = 2; i2 }
    }
    override def longIterator: LongIterator = LongIterator(i1, i2)

    override def foreach(f: LongConsumer): Unit = { f.accept(i1); f.accept(i2) }
    override def foreachPair[U](f: (Long, Long) ⇒ U): Unit = { f(i1, i2) }
    override def filter(p: (Long) ⇒ Boolean): LongTrieSet = {
        if (p(i1)) {
            if (p(i2))
                this
            else
                LongTrieSet1(i1)
        } else {
            if (p(i2))
                LongTrieSet1(i2)
            else
                EmptyLongTrieSet
        }
    }
    override def withFilter(p: (Long) ⇒ Boolean): LongTrieSet = new FilteredLongTrieSet(this, p)
    override def map(f: Long ⇒ Long): LongTrieSet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        if (newI1 != i1 || newI2 != i2)
            LongTrieSet(newI1, newI2)
        else
            this
    }

    override def flatMap(f: Long ⇒ LongTrieSet): LongTrieSet = f(i1) ++ f(i2)

    override def -(i: Long): LongTrieSet = {
        if (i == i1) LongTrieSet1(i2)
        else if (i == i2) LongTrieSet1(i1)
        else this
    }
    override def +(i: Long): LongTrieSet = if (i1 == i | i2 == i) this else LongTrieSet.from(i1, i2, i)
    override def +!(i: Long): LongTrieSet = this + i
    override def intersect(other: LongTrieSet): LongTrieSet = {
        other.size match {
            case 0 ⇒ other
            case 1 ⇒ if (other.head == i1 || other.head == i2) other else EmptyLongTrieSet
            case _ ⇒
                if (other.contains(this.i1)) {
                    if (other.contains(this.i2)) {
                        this
                    } else {
                        LongTrieSet1(this.i1)
                    }
                } else if (other.contains(this.i2)) {
                    LongTrieSet1(this.i2)
                } else {
                    EmptyLongTrieSet
                }
        }
    }
    override def contains(value: Long): Boolean = value == i1 || value == i2
    override def exists(p: Long ⇒ Boolean): Boolean = p(i1) || p(i2)
    override def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B = f(f(z, i1), i2)
    override def forall(f: Long ⇒ Boolean): Boolean = f(i1) && f(i2)

    override private[immutable] def subsetOf(other: LongTrieSet, level: Long): Boolean = {
        other.size match {
            case 0 | 1 ⇒
                false
            case 2 ⇒
                other match {
                    case that: LongTrieSet2 ⇒
                        that.i1 == this.i1 && that.i2 == this.i2
                    case _ ⇒
                        // ... this case should never occur...
                        other.contains(i1, i1 >>> level) && other.contains(i2, i2 >>> level)
                }
            case _ ⇒
                other.contains(i1, i1 >>> level) && other.contains(i2, i2 >>> level)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSet2 ⇒ this.i1 == that.i1 && this.i2 == that.i2
            case that: LongTrieSet  ⇒ that.size == 2 && that.contains(i1) && that.contains(i2)
            case _                  ⇒ false
        }
    }

    override def hashCode: Int = 31 * (31 + JLong.hashCode(i1)) + JLong.hashCode(i2) // compatible to Arrays.hashCode

    override private[immutable] def +(i: Long, level: Long): LongTrieSet = this.+(i)
    override private[immutable] def +!(i: Long, level: Long): LongTrieSet = this.+!(i)
}

/**
 * Represents an ordered set of three long values: i1 < i2 < i3.
 */
private[immutable] final class LongTrieSet3 private[immutable] (
        val i1: Long, val i2: Long, val i3: Long
) extends LongTrieSetL {

    override def isEmpty: Boolean = false
    override def isSingletonSet: Boolean = false
    override def hasMultipleElements: Boolean = true
    override def size: Int = 3
    override def getAndRemove: LongHeadAndRestOfSet[LongTrieSet] = {
        LongHeadAndRestOfSet(i3, new LongTrieSet2(i1, i2))
    }
    override def head: Long = i1
    override def flatMap(f: Long ⇒ LongTrieSet): LongTrieSet = f(i1) ++ f(i2) ++ f(i3)
    override def iterator: Iterator[Long] = new AbstractIterator[Long] {
        var i = 0
        def hasNext: Boolean = i < 3
        def next: Long = {
            val v = i
            i += 1
            v match {
                case 0 ⇒ i1
                case 1 ⇒ i2
                case 2 ⇒ i3
                case _ ⇒ throw new IllegalStateException()
            }
        }
    }
    override def longIterator: LongIterator = LongIterator(i1, i2, i3)

    override def foreach(f: LongConsumer): Unit = { f.accept(i1); f.accept(i2); f.accept(i3) }
    override def foreachPair[U](f: (Long, Long) ⇒ U): Unit = { f(i1, i2); f(i1, i3); f(i2, i3) }

    override def filter(p: (Long) ⇒ Boolean): LongTrieSet = {
        if (p(i1)) {
            if (p(i2)) {
                if (p(i3))
                    this
                else
                    new LongTrieSet2(i1, i2)
            } else {
                if (p(i3))
                    new LongTrieSet2(i1, i3)
                else
                    LongTrieSet1(i1)
            }
        } else {
            if (p(i2)) {
                if (p(i3))
                    new LongTrieSet2(i2, i3)
                else
                    LongTrieSet1(i2)
            } else {
                if (p(i3))
                    LongTrieSet1(i3)
                else
                    EmptyLongTrieSet
            }
        }
    }
    override def withFilter(p: (Long) ⇒ Boolean): LongTrieSet = new FilteredLongTrieSet(this, p)
    override def map(f: Long ⇒ Long): LongTrieSet = {
        val i1 = this.i1
        val newI1 = f(i1)
        val i2 = this.i2
        val newI2 = f(i2)
        val i3 = this.i3
        val newI3 = f(i3)
        if (newI1 != i1 || newI2 != i2 || newI3 != i3)
            LongTrieSet(newI1, newI2, newI3) // ensures invariant
        else
            this
    }

    override def -(i: Long): LongTrieSet = {
        if (i1 == i) new LongTrieSet2(i2, i3)
        else if (i2 == i) new LongTrieSet2(i1, i3)
        else if (i3 == i) new LongTrieSet2(i1, i2)
        else this
    }
    override def +(i: Long): LongTrieSet = this.+(i, 0)
    override def +!(i: Long): LongTrieSet = this.+(i, 0)
    override def contains(value: Long): Boolean = value == i1 || value == i2 || value == i3
    override def exists(p: Long ⇒ Boolean): Boolean = p(i1) || p(i2) || p(i3)
    override def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B = f(f(f(z, i1), i2), i3)
    override def forall(f: Long ⇒ Boolean): Boolean = f(i1) && f(i2) && f(i3)

    override private[immutable] def subsetOf(other: LongTrieSet, level: Long): Boolean = {
        other.size match {
            case 0 | 1 | 2 ⇒
                false
            case 3 ⇒
                other match {
                    case that: LongTrieSet3 ⇒
                        that.i1 == this.i1 && that.i2 == this.i2 && that.i3 == this.i3
                    case _ ⇒
                        // ... this case should never occur...
                        other.contains(i1, i1 >>> level) &&
                            other.contains(i2, i2 >>> level) &&
                            other.contains(i3, i3 >>> level)
                }
            case _ ⇒
                other.contains(i1, i1 >>> level) &&
                    other.contains(i2, i2 >>> level) &&
                    other.contains(i3, i3 >>> level)
        }
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSet3 ⇒
                this.i1 == that.i1 && this.i2 == that.i2 && this.i3 == that.i3
            case that: LongTrieSet ⇒
                that.size == 3 && that.contains(i1) && that.contains(i2) && that.contains(i3)
            case _ ⇒
                false
        }
    }

    override def hashCode: Int = {
        // compatible to Arrays.hashCode
        31 * (31 * (31 + JLong.hashCode(i1)) + JLong.hashCode(i2)) + JLong.hashCode(i3)
    }

    override private[immutable] def +(i: Long, level: Long): LongTrieSet = {
        if (i == i1 || i == i2 || i == i3)
            this
        else
            LongTrieSet.from(i, i1, i2, i3, level)
    }
    override private[immutable] def +!(i: Long, level: Long): LongTrieSet = this.+(i, level)
}

private[immutable] abstract class LongTrieSetNN extends LongTrieSet {

    final override def isSingletonSet: Boolean = size == 1
    final override def hasMultipleElements: Boolean = size > 1
    final override def isEmpty: Boolean = false

    final override def map(f: Long ⇒ Long): LongTrieSet = {
        foldLeft(EmptyLongTrieSet: LongTrieSet)(_ +! f(_))
    }

    final override def flatMap(f: Long ⇒ LongTrieSet): LongTrieSet = {
        foldLeft(EmptyLongTrieSet: LongTrieSet)(_ ++! f(_))
    }

    final override def withFilter(p: Long ⇒ Boolean): LongTrieSet = new FilteredLongTrieSet(this, p)

    final override def equals(other: Any): Boolean = {
        other match {
            case that: LongTrieSet ⇒
                that.size == this.size && {
                    // we have stable orderings!
                    val thisIt = this.longIterator
                    val otherIt = that.longIterator
                    var allEqual = true
                    while (thisIt.hasNext && allEqual) {
                        allEqual = thisIt.next() == otherIt.next()
                    }
                    allEqual
                }
            case _ ⇒
                false
        }
    }

    final override def hashCode: Int = foldLeft(1)(31 * _ + JLong.hashCode(_))
}

private[immutable] final class LongTrieSetN private[immutable] (
        private[immutable] var left:  LongTrieSet, // can be empty, but never null!
        private[immutable] var right: LongTrieSet, // can be empty, but never null!
        var size:                     Int
) extends LongTrieSetNN { longSet ⇒

    assert(left.size + right.size == size)
    assert(size > 0) // <= can be "one" at construction time

    override def head: Long = if (left.nonEmpty) left.head else right.head

    override def exists(p: Long ⇒ Boolean): Boolean = left.exists(p) || right.exists(p)
    override def forall(p: Long ⇒ Boolean): Boolean = left.forall(p) && right.forall(p)

    override private[immutable] def subsetOf(other: LongTrieSet, level: Long): Boolean = {
        if (this.size > other.size)
            return false;

        other match {
            case that: LongTrieSetN ⇒
                this.right.size <= that.right.size && // check if we have a chance...
                    this.left.subsetOf(that.left, level + 1) &&
                    this.right.subsetOf(that.right, level + 1)
            case that: LongTrieSetNJustLeft ⇒
                this.right.isEmpty && this.left.subsetOf(that.left, level + 1)
            case that: LongTrieSetNJustRight ⇒
                this.left.isEmpty && this.right.subsetOf(that.right, level + 1)
            case that ⇒
                // Here, the level is actually not relevant...
                this.left.subsetOf(that, level + 1) && this.right.subsetOf(that, level + 1)
        }
    }

    override def foreach(f: LongConsumer): Unit = {
        left.foreach(f)
        right.foreach(f)
    }
    override def foreachPair[U](f: (Long, Long) ⇒ U): Unit = {
        val is = longIterator.toArray(size = size)
        val max = size
        var i = 0
        while (i < max) {
            var j = i + 1
            while (j < max) {
                f(is(i), is(j))
                j += 1
            }
            i += 1
        }
    }

    override def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B = {
        right.foldLeft(left.foldLeft(z)(f))(f)
    }

    override private[immutable] def +(i: Long, level: Long): LongTrieSet = {
        if (((i >>> level) & 1) == 0) {
            val left = this.left
            val newLeft = left + (i, level + 1)
            if (newLeft eq left)
                this
            else
                LongTrieSetN(newLeft, right, size + 1)
        } else {
            val right = this.right
            val newRight = right + (i, level + 1)
            if (newRight eq right)
                this
            else
                LongTrieSetN(left, newRight, size + 1)
        }
    }

    override def +(i: Long): LongTrieSet = this.+(i, 0)

    override private[immutable] def +!(i: Long, level: Long): LongTrieSet = {
        if (((i >>> level) & 1) == 0) {
            val oldLeft = this.left
            val oldLeftSize = oldLeft.size
            val newLeft = oldLeft +! (i, level + 1)
            if (newLeft.size != oldLeftSize) {
                this.left = newLeft
                this.size += 1
            }
        } else {
            val oldRight = this.right
            val oldRightSize = oldRight.size
            val newRight = oldRight +! (i, level + 1)
            if (newRight.size != oldRightSize) {
                this.right = newRight
                this.size += 1
            }
        }
        this
    }

    override def +!(i: Long): LongTrieSet = this.+!(i, 0)

    override private[immutable] def contains(value: Long, key: Long): Boolean = {
        if ((key & 1) == 0)
            left.contains(value, key >>> 1)
        else
            right.contains(value, key >>> 1)
    }

    override def contains(value: Long): Boolean = this.contains(value, value)

    /**
     * Ensures that subtrees which contain less than 3 elements are represented using
     * a cannonical representation.
     */
    override private[immutable] def constringe(): LongTrieSet = {
        assert(size <= 2)
        if (left.isEmpty)
            right.constringe()
        else if (right.isEmpty)
            left.constringe()
        else
            new LongTrieSet2(left.head, right.head)
    }

    private[immutable] def -(i: Long, key: Long): LongTrieSet = {
        if ((key & 1) == 0) {
            val left = this.left
            val newLeft = left.-(i, key >>> 1)
            if (newLeft eq left)
                this
            else {
                (size - 1) match {
                    case 0 ⇒
                        EmptyLongTrieSet
                    case 1 ⇒
                        if (newLeft.isEmpty)
                            right.constringe()
                        else
                            newLeft.constringe()
                    case newSize ⇒
                        LongTrieSetN(newLeft, right, newSize)
                }
            }
        } else {
            val right = this.right
            val newRight = right.-(i, key >>> 1)
            if (newRight eq right)
                this
            else {
                val newSize = size - 1
                if (newSize == 0)
                    EmptyLongTrieSet
                else if (newSize == 1) {
                    if (newRight.isEmpty)
                        left.constringe()
                    else
                        newRight.constringe()
                } else {
                    LongTrieSetN(left, newRight, newSize)
                }
            }
        }
    }

    def -(i: Long): LongTrieSet = this.-(i, i)

    def longIterator: LongIterator = {
        new LongIterator {
            private[this] var it: LongIterator = left.longIterator
            private[this] var isRightIterator: Boolean = false
            private[this] def checkIterator(): Unit = {
                if (!it.hasNext && !isRightIterator) {
                    isRightIterator = true
                    it = right.longIterator
                }
            }
            override def toSet: LongTrieSet = longSet
            checkIterator()
            def hasNext: Boolean = it.hasNext
            def next(): Long = { val v = it.next(); checkIterator(); v }
        }
    }

    def iterator: Iterator[Long] = {
        new AbstractIterator[Long] {
            private[this] val it = longIterator
            override def hasNext: Boolean = it.hasNext
            override def next(): Long = it.next()
        }
    }

    override def getAndRemove: LongHeadAndRestOfSet[LongTrieSet] = {
        // try to reduce the tree size by removing an element from the
        // bigger subtree
        val left = this.left
        val right = this.right
        val leftSize = left.size
        val rightSize = right.size
        if (leftSize > rightSize) {
            // => left has at least one element
            if (leftSize == 1) { // => right is empty!
                LongHeadAndRestOfSet(left.head, EmptyLongTrieSet)
            } else {
                val LongHeadAndRestOfSet(v, newLeft) = left.getAndRemove
                val theNewLeft = if (leftSize == 2) newLeft.constringe() else newLeft
                LongHeadAndRestOfSet(v, LongTrieSetN(theNewLeft, right, leftSize - 1 + rightSize))
            }
        } else {
            // ...leftSize <= right.size
            assert(right.nonEmpty)
            if (right.isSingletonSet) {
                // left.size \in {0,1}
                LongHeadAndRestOfSet(right.head, left.constringe())
            } else {
                val LongHeadAndRestOfSet(v, newRight) = right.getAndRemove
                val theNewRight = if (rightSize == 2) newRight.constringe() else newRight
                LongHeadAndRestOfSet(v, LongTrieSetN(left, theNewRight, size - 1))
            }
        }
    }

    override def filter(p: Long ⇒ Boolean): LongTrieSet = {
        val left = this.left
        val right = this.right
        var newLeft = left.filter(p)
        var newRight = right.filter(p)
        if ((newLeft eq left) && (newRight eq right))
            return this;

        val newLeftSize = newLeft.size
        val newRightSize = newRight.size

        if (newLeftSize + newRightSize <= 2) {
            val newSet =
                if (newLeft.isEmpty) newRight
                else if (newRight.isEmpty) newLeft
                else newLeft + newRight.head
            return newSet;
        }

        if (newLeftSize <= 2) {
            newLeft = newLeft.constringe()
        }
        if (newRightSize <= 2) {
            newRight = newRight.constringe()
        }
        LongTrieSetN(newLeft, newRight, newLeftSize + newRightSize)
    }

}

private[immutable] object LongTrieSetN {

    def apply(
        left:  LongTrieSet, // can be empty, but never null!
        right: LongTrieSet, // can be empty, but never null!
        size:  Int
    ): LongTrieSet = {
        if (right.isEmpty)
            new LongTrieSetNJustLeft(left)
        else if (left.isEmpty)
            new LongTrieSetNJustRight(right)
        else
            new LongTrieSetN(left, right, size)
    }
}

private[immutable] final class LongTrieSetNJustRight private[immutable] (
        private[immutable] var right: LongTrieSet // can't be empty, left is already empty
) extends LongTrieSetNN { longSet ⇒

    assert(size > 0) // <= can be "one" at construction time

    override def size: Int = right.size
    override def head: Long = right.head
    override def exists(p: Long ⇒ Boolean): Boolean = right.exists(p)
    override def forall(p: Long ⇒ Boolean): Boolean = right.forall(p)
    override def foreach(f: LongConsumer): Unit = right.foreach(f)
    override def foreachPair[U](f: (Long, Long) ⇒ U): Unit = right.foreachPair(f)
    override def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B = right.foldLeft(z)(f)

    override private[immutable] def +(i: Long, level: Long): LongTrieSet = {
        if (((i >>> level) & 1) == 0) {
            LongTrieSetN(LongTrieSet1(i), right, size + 1)
        } else {
            val right = this.right
            val newRight = right + (i, level + 1)
            if (newRight eq right)
                this
            else
                new LongTrieSetNJustRight(newRight)
        }
    }

    override private[immutable] def +!(i: Long, level: Long): LongTrieSet = {
        if (((i >>> level) & 1) == 0) {
            LongTrieSetN(LongTrieSet1(i), right, size + 1)
        } else {
            this.right = this.right +! (i, level + 1)
            this
        }
    }

    override def +(i: Long): LongTrieSet = this.+(i, 0)
    override def +!(i: Long): LongTrieSet = this.+!(i, 0)

    override private[immutable] def subsetOf(other: LongTrieSet, level: Long): Boolean = {
        if (this.size > other.size)
            return false;

        other match {
            case that: LongTrieSetN          ⇒ this.right.subsetOf(that.right, level + 1)
            case that: LongTrieSetNJustLeft  ⇒ false
            case that: LongTrieSetNJustRight ⇒ this.right.subsetOf(that.right, level + 1)
            case that                        ⇒ this.right.subsetOf(that, level + 1)
        }
    }

    override private[immutable] def contains(value: Long, key: Long): Boolean = {
        if ((key & 1) == 0)
            false
        else
            right.contains(value, key >>> 1)
    }

    override def contains(value: Long): Boolean = this.contains(value, value)

    /**
     * Ensures that subtrees which contain less than 3 elements are represented using
     * a cannonical representation.
     */
    override private[immutable] def constringe(): LongTrieSet = {
        assert(size <= 2)
        right.constringe()
    }

    private[immutable] def -(i: Long, key: Long): LongTrieSet = {
        if ((key & 1) == 0) {
            this
        } else {
            val right = this.right
            val newRight = right.-(i, key >>> 1)
            if (newRight eq right)
                this
            else {
                val newSize = size - 1
                if (newSize == 0)
                    EmptyLongTrieSet
                else if (newSize == 1) {
                    newRight.constringe()
                } else {
                    new LongTrieSetNJustRight(newRight)
                }
            }
        }
    }

    def -(i: Long): LongTrieSet = this.-(i, i)

    def longIterator: LongIterator = right.longIterator
    def iterator: Iterator[Long] = right.iterator

    override def getAndRemove: LongHeadAndRestOfSet[LongTrieSet] = {
        // try to reduce the tree size by removing an element from the
        // bigger subtree
        val right = this.right
        val rightSize = right.size
        if (right.isSingletonSet) {
            LongHeadAndRestOfSet(right.head, EmptyLongTrieSet)
        } else {
            val LongHeadAndRestOfSet(v, newRight) = right.getAndRemove
            val theNewRight = if (rightSize == 2) newRight.constringe() else newRight
            LongHeadAndRestOfSet(v, new LongTrieSetNJustRight(theNewRight))
        }

    }

    override def filter(p: Long ⇒ Boolean): LongTrieSet = {
        val right = this.right
        val newRight = right.filter(p)
        if (newRight eq right)
            return this;

        val newRightSize = newRight.size
        if (newRightSize <= 2) {
            newRight.constringe()
        } else {
            new LongTrieSetNJustRight(newRight)
        }
    }

}

private[immutable] final class LongTrieSetNJustLeft private[immutable] (
        private[immutable] var left: LongTrieSet // cannot be empty; right is empty
) extends LongTrieSetNN { longSet ⇒

    assert(size > 0) // <= can be "one" at construction time

    override def size: Int = left.size

    override def head: Long = left.head
    override def exists(p: Long ⇒ Boolean): Boolean = left.exists(p)
    override def forall(p: Long ⇒ Boolean): Boolean = left.forall(p)
    override def foreach(f: LongConsumer): Unit = left.foreach(f)
    override def foreachPair[U](f: (Long, Long) ⇒ U): Unit = left.foreachPair(f)
    override def foldLeft[B](z: B)(f: (B, Long) ⇒ B): B = left.foldLeft(z)(f)

    override private[immutable] def +(i: Long, level: Long): LongTrieSet = {
        if (((i >>> level) & 1) == 0) {
            val left = this.left
            val newLeft = left + (i, level + 1)
            if (newLeft eq left)
                this
            else
                new LongTrieSetNJustLeft(newLeft)
        } else {
            new LongTrieSetN(left, LongTrieSet1(i), size + 1)
        }
    }

    override private[immutable] def +!(i: Long, level: Long): LongTrieSet = {
        if (((i >>> level) & 1) == 0) {
            this.left = this.left +! (i, level + 1)
            this
        } else {
            new LongTrieSetN(left, LongTrieSet1(i), size + 1)
        }
    }

    override def +(i: Long): LongTrieSet = this.+(i, 0)
    override def +!(i: Long): LongTrieSet = this.+!(i, 0)

    override def subsetOf(other: LongTrieSet, level: Long): Boolean = {
        if (this.size > other.size)
            return false;

        other match {
            case that: LongTrieSetN          ⇒ this.left.subsetOf(that.left, level + 1)
            case that: LongTrieSetNJustLeft  ⇒ this.left.subsetOf(that.left, level + 1)
            case that: LongTrieSetNJustRight ⇒ false
            case that                        ⇒ this.left.subsetOf(that, level + 1)
        }
    }

    override private[immutable] def contains(value: Long, key: Long): Boolean = {
        if ((key & 1) == 0)
            left.contains(value, key >>> 1)
        else
            false
    }

    override def contains(value: Long): Boolean = this.contains(value, value)

    /**
     * Ensures that subtrees which contain less than 3 elements are represented using
     * a cannonical representation.
     */
    override private[immutable] def constringe(): LongTrieSet = {
        assert(size <= 2)
        left.constringe()
    }

    private[immutable] def -(i: Long, key: Long): LongTrieSet = {
        if ((key & 1) == 0) {
            val left = this.left
            val newLeft = left.-(i, key >>> 1)
            if (newLeft eq left)
                this
            else {
                val newSize = size - 1
                newSize match {
                    case 0 ⇒
                        EmptyLongTrieSet
                    case 1 ⇒
                        newLeft.constringe()
                    case _ ⇒
                        new LongTrieSetNJustLeft(newLeft)
                }
            }
        } else {
            this
        }
    }

    def -(i: Long): LongTrieSet = this.-(i, i)

    def longIterator: LongIterator = left.longIterator

    def iterator: Iterator[Long] = left.iterator

    override def getAndRemove: LongHeadAndRestOfSet[LongTrieSet] = {
        // try to reduce the tree size by removing an element from the
        // bigger subtree
        val left = this.left
        val leftSize = left.size
        if (leftSize == 1) { // => right is empty!
            LongHeadAndRestOfSet(left.head, EmptyLongTrieSet)
        } else {
            val LongHeadAndRestOfSet(v, newLeft) = left.getAndRemove
            val theNewLeft = if (leftSize == 2) newLeft.constringe() else newLeft
            LongHeadAndRestOfSet(v, new LongTrieSetNJustLeft(theNewLeft))
        }

    }

    override def filter(p: (Long) ⇒ Boolean): LongTrieSet = {
        val left = this.left
        val newLeft = left.filter(p)
        if (newLeft eq left)
            return this;

        val newLeftSize = newLeft.size
        if (newLeftSize <= 2) {
            newLeft.constringe()
        } else {
            new LongTrieSetNJustLeft(newLeft)
        }
    }

}

class LongTrieSetBuilder extends scala.collection.mutable.Builder[Long, LongTrieSet] {
    private[this] var s: LongTrieSet = EmptyLongTrieSet
    def +=(i: Long): this.type = { s +!= i; this }
    def clear(): Unit = s = EmptyLongTrieSet
    def result(): LongTrieSet = s
}

/**
 * Factory to create LongTrieSets.
 */
object LongTrieSet {

    def empty: LongTrieSet = EmptyLongTrieSet

    def apply(i1: Long): LongTrieSet = LongTrieSet1(i1)

    def apply(i1: Long, i2: Long): LongTrieSet = {
        if (i1 == i2)
            LongTrieSet1(i1)
        else {
            from(i1, i2)
        }
    }

    /** Constructs a new LongTrie from the two distinct(!) values. */
    def from(i1: Long, i2: Long): LongTrieSet = {
        assert(i1 != i2)
        // we have to ensure the same ordering as used when the values are
        // stored in the trie
        if ((JLong.lowestOneBit(i1 ^ i2) & i1) == 0) {
            // ... i2 is the value with a 0 at the bit position where both values differ
            new LongTrieSet2(i1, i2)
        } else {
            new LongTrieSet2(i2, i1)
        }
    }

    def apply(i1: Long, i2: Long, i3: Long): LongTrieSet = {
        if (i1 == i2)
            LongTrieSet(i1, i3) // this also handles the case i1 == i3
        else if (i1 == i3 || i2 == i3) { // we have i1 =!= i2
            LongTrieSet.from(i1, i2)
        } else { // i1 =!= i2 && i2 =!= i3 && i1 =!= i3
            LongTrieSet.from(i1, i2, i3)
        }
    }

    /** Constructs a new LongTrie from the three distinct(!) values! */
    def from(i1: Long, i2: Long, i3: Long): LongTrieSet = {
        // We have to ensure the same ordering as used when the values are stored in the trie...
        var v1, v2, v3 = 0L
        if ((JLong.lowestOneBit(i1 ^ i2) & i1) == 0) {
            // ... i1 is the value with a 0 at the lowest one bit position...
            v1 = i1
            v2 = i2
        } else {
            v1 = i2
            v2 = i1
        }

        if ((JLong.lowestOneBit(v2 ^ i3) & v2) == 0) {
            // v2 is the value with the 0 and the distinguishing position...
            v3 = i3
        } else {
            v3 = v2
            if ((JLong.lowestOneBit(v1 ^ i3) & v1) == 0) {
                v2 = i3
            } else {
                v2 = v1
                v1 = i3
            }
        }

        new LongTrieSet3(v1, v2, v3)
    }

    def apply(i1: Long, i2: Long, i3: Long, i4: Long): LongTrieSet = {
        if (i1 == i2) {
            LongTrieSet(i2, i3, i4)
        } else if (i1 == i3 || i2 == i3 || i3 == i4) { // we have i1 =!= i2
            LongTrieSet(i1, i2, i4)
        } else if (i1 == i4 || i2 == i4) {
            LongTrieSet(i1, i2, i3)
        } else {
            LongTrieSet.from(i1, i2, i3, i4, 0)
        }
    }

    def from(i1: Long, i2: Long, i3: Long, i4: Long): LongTrieSet = {
        if ((i1 & 1L) == 0L) {
            if ((i2 & 1L) == 0L) {
                if ((i3 & 1L) == 0L) {
                    if ((i4 & 1L) == 0L) { // first bit of all "0"
                        new LongTrieSetNJustLeft(from(i1, i2, i3, i4, 1))
                    } else { // first bit of i4 is "1"
                        new LongTrieSetN(LongTrieSet.from(i1, i2, i3), LongTrieSet1(i4), 4)
                    }
                } else {
                    if ((i4 & 1L) == 0L) { // first bit of i3 is "1"
                        new LongTrieSetN(LongTrieSet.from(i1, i2, i4), LongTrieSet1(i3), 4)
                    } else { // first bit of i3, i4 is "1"
                        new LongTrieSetN(LongTrieSet.from(i1, i2), LongTrieSet.from(i3, i4), 4)
                    }
                }
            } else {
                if ((i3 & 1L) == 0L) {
                    if ((i4 & 1L) == 0L) { // first bit of i2 is "1"
                        new LongTrieSetN(LongTrieSet.from(i1, i3, i4), LongTrieSet1(i2), 4)
                    } else { // first bit of i2 and i4 is "1"
                        new LongTrieSetN(LongTrieSet.from(i1, i3), LongTrieSet.from(i2, i4), 4)
                    }
                } else {
                    if ((i4 & 1L) == 0L) { // first bit of i2, i3 is "1"
                        new LongTrieSetN(LongTrieSet.from(i1, i4), LongTrieSet.from(i2, i3), 4)
                    } else { // first bit of i2, i3, i4 is "1"
                        new LongTrieSetN(LongTrieSet1(i1), LongTrieSet.from(i2, i3, i4), 4)
                    }
                }
            }
        } else {
            if ((i2 & 1L) == 0L) {
                if ((i3 & 1L) == 0L) {
                    if ((i4 & 1L) == 0L) { // first bit of i1 is "1"
                        new LongTrieSetN(LongTrieSet.from(i2, i3, i4), LongTrieSet1(i1), 4)
                    } else { // first bit of i1, i4 is "1"
                        new LongTrieSetN(LongTrieSet.from(i2, i3), LongTrieSet.from(i1, i4), 4)
                    }
                } else {
                    if ((i4 & 1L) == 0L) { // first bit of i1, i3 is "1"
                        new LongTrieSetN(LongTrieSet.from(i2, i4), LongTrieSet.from(i1, i3), 4)
                    } else { // first bit of i1, i3, i4 is "1"
                        new LongTrieSetN(LongTrieSet1(i2), LongTrieSet.from(i1, i3, i4), 4)
                    }
                }
            } else {
                if ((i3 & 1L) == 0L) {
                    if ((i4 & 1L) == 0L) { // first bit of i1, i2 is "1"
                        new LongTrieSetN(LongTrieSet.from(i3, i4), LongTrieSet.from(i1, i2), 4)
                    } else { // first bit of i1, i2 and i4 is "1"
                        new LongTrieSetN(LongTrieSet1(i3), LongTrieSet.from(i1, i2, i4), 4)
                    }
                } else {
                    if ((i4 & 1L) == 0L) { // first bit of i1, i2, i3 is "1"
                        new LongTrieSetN(LongTrieSet1(i4), LongTrieSet.from(i1, i2, i3), 4)
                    } else { // first bit of i1, i2, i3, i4 is "1"
                        new LongTrieSetNJustRight(from(i1, i2, i3, i4, 1))
                    }
                }
            }
        }
    }

    /**
     * Constructs a new `LongTrieSet` from the given distinct values.
     *
     * If level is > 0 then all values have to have the same least significant bits up until level!
     */
    private[immutable] def from(i1: Long, i2: Long, i3: Long, i4: Long, level: Long): LongTrieSet = {
        val root =
            if (((i1 >>> level) & 1L) == 0L)
                new LongTrieSetNJustLeft(LongTrieSet1(i1))
            else
                new LongTrieSetNJustRight(LongTrieSet1(i1))

        root +! (i2, level) +! (i3, level) +! (i4, level)
    }

}
