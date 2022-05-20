/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj
package collection
package immutable

import scala.collection.mutable
import scala.collection.immutable.ArraySeq
import scala.collection.mutable.Builder
import scala.reflect.ClassTag

/**
 * An '''unordered''' trie-set based on the unique ids of the stored [[UID]] objects. I.e.,
 * equality of two sets is defined in terms of the unique ids and not in terms of structural or
 * reference equality of the stored elements.
 *
 * ==Implementation==
 * This trie set uses the least significant bit to decide whether the search is continued in the
 * right or left branch.
 *
 * Small sets are represented using a UIDSet0...3.
 *
 * Compared to Scala's `Set` implementations in particular the tail and filter methods are much
 * faster.
 */
sealed abstract class UIDSet[T <: UID]
    extends scala.collection.immutable.Set[T]
    with scala.collection.immutable.StrictOptimizedSetOps[T, Set, UIDSet[T]] { set =>

    final override def empty: UIDSet[T] = UIDSet0.asInstanceOf[UIDSet[T]]
    final override def contains(e: T): Boolean = containsId(e.id)
    override def exists(p: (T) => Boolean): Boolean
    override def forall(p: (T) => Boolean): Boolean
    override def head: T
    /**
     * Returns the current last value, which is never head if the underlying set contains
     * at least two values. The last value can be different for two sets containing
     * the same values if both sets were created in different ways.
     */
    override def last: T
    override def tail: UIDSet[T] = throw new UnknownError()
    override def incl(e: T): UIDSet[T]
    override def excl(e: T): UIDSet[T]
    override def foldLeft[B](z: B)(op: (B, T) => B): B
    override def fromSpecific(coll: IterableOnce[T]): UIDSet[T] = UIDSet.fromSpecific(coll)
    override def newSpecificBuilder: mutable.Builder[T, UIDSet[T]] = UIDSet.newBuilder[T]
    def mapUIDSet[B <: UID](f: T => B): UIDSet[B] = UIDSet.fromSpecific(this.iterator.map(f))

    //
    // METHODS DEFINED BY UIDSet
    //

    /** Iterator over all ids. */
    def idIterator: IntIterator

    def foreachIterator: ForeachRefIterator[T] = new ForeachRefIterator[T] {
        def foreach[U](f: T => U): Unit = set.foreach(f)
    }

    override def iterator: Iterator[T]

    def idSet: IntTrieSet

    def containsId(id: Int): Boolean

    def isSingletonSet: Boolean

    def ++(es: UIDSet[T]): UIDSet[T]
    def findById(id: Int): Option[T]

    /**
     * Adds the given element to this set by mutating it!
     */
    private[opalj] def addMutate(e: T): UIDSet[T] = this.incl(e)

    // The following method(s) is(are) unsafe if "add!" is used!
    final def toUIDSet[X >: T <: UID]: UIDSet[X] = this.asInstanceOf[UIDSet[X]]
    final def includes[X >: T <: UID](e: X): Boolean = containsId(e.id)
    final def add[X >: T <: UID](e: X): UIDSet[X] = {
        (this.incl(e.asInstanceOf[T] /*pure fiction*/ )).asInstanceOf[UIDSet[X] /*pure fiction*/ ]
    }

    /**
     * Performs a qualified comparison of this set with the given set.
     */
    def compare(that: UIDSet[T]): SetRelation = {
        val thisSize = this.size
        val thatSize = that.size

        if (thisSize < thatSize) {
            if (this.forall(e => that.containsId(e.id))) StrictSubset else UncomparableSets
        } else if (thisSize == thatSize) {
            if (this == that) EqualSets else UncomparableSets
        } else if (that.forall(e => this.containsId(e.id))) {
            StrictSuperset
        } else
            UncomparableSets
    }

    def toArraySeq(implicit classTag: ClassTag[T]): ArraySeq[T] = ArraySeq.unsafeWrapArray(toArray[T])

}

/**
 * Represents the empty UIDSet.
 */
object UIDSet0 extends UIDSet[UID] {

    override def isEmpty: Boolean = true
    override def size: Int = 0

    override def find(p: UID => Boolean): Option[UID] = None
    override def exists(p: UID => Boolean): Boolean = false
    override def forall(p: UID => Boolean): Boolean = true
    override def foreach[U](f: UID => U): Unit = {}
    override def iterator: Iterator[Nothing] = Iterator.empty
    override def head: UID = throw new NoSuchElementException
    override def last: UID = throw new NoSuchElementException
    override def headOption: Option[UID] = None
    override def tail: UIDSet[UID] = throw new NoSuchElementException
    override def filter(p: UID => Boolean): UIDSet[UID] = this
    override def filterNot(p: UID => Boolean): UIDSet[UID] = this
    override def incl(e: UID): UIDSet[UID] = UIDSet1(e)
    override def excl(e: UID): UIDSet[UID] = this
    override def foldLeft[B](z: B)(op: (B, UID) => B): B = z
    override def drop(n: Int): UIDSet[UID] = this
    // default equals/hashCode are a perfect fit

    //
    // METHODS DEFINED BY UIDSet
    //
    override def findById(id: Int): Option[UID] = None
    override def idIterator: IntIterator = IntIterator.empty
    override def foreachIterator: ForeachRefIterator[Nothing] = ForeachRefIterator.empty
    override def idSet: IntTrieSet = IntTrieSet.empty
    override def containsId(id: Int): Boolean = false
    override def isSingletonSet: Boolean = false
    override def ++(es: UIDSet[UID]): UIDSet[UID] = es
    override def compare(that: UIDSet[UID]): SetRelation = {
        if (that.isEmpty) EqualSets else /* this is a */ StrictSubset
    }
    override def mapUIDSet[B <: UID](f: UID => B): UIDSet[B] = UIDSet.empty

}

sealed abstract class NonEmptyUIDSet[T <: UID] extends UIDSet[T] {

    final override def isEmpty: Boolean = false
    final override def headOption: Option[T] = Some(head)
}

final case class UIDSet1[T <: UID](value: T) extends NonEmptyUIDSet[T] {

    override def size: Int = 1
    override def find(p: T => Boolean): Option[T] = if (p(value)) Some(value) else None
    override def exists(p: T => Boolean): Boolean = p(value)
    override def forall(p: T => Boolean): Boolean = p(value)
    override def foreach[U](f: T => U): Unit = f(value)
    override def head: T = value
    override def last: T = value
    override def tail: UIDSet[T] = empty
    override def iterator: Iterator[T] = Iterator(value)
    override def filter(p: T => Boolean): UIDSet[T] = if (p(value)) this else empty
    override def filterNot(p: T => Boolean): UIDSet[T] = if (p(value)) empty else this
    override def incl(e: T): UIDSet[T] = if (value.id == e.id) this else new UIDSet2(value, e)
    override def excl(e: T): UIDSet[T] = if (value.id == e.id) empty else this
    override def foldLeft[B](z: B)(op: (B, T) => B): B = op(z, value)
    override def drop(n: Int): UIDSet[T] = if (n == 0) this else empty

    override def hashCode(): Int = value.id
    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet[_] => that.size == 1 && that.head.id == value.id
            case _               => false
        }
    }

    //
    // METHODS DEFINED BY UIDSet
    //

    override def findById(id: Int): Option[T] = if (value.id == id) Some(value) else None
    override def idIterator: IntIterator = IntIterator(value.id)
    override def idSet: IntTrieSet = IntTrieSet1(value.id)
    override def isSingletonSet: Boolean = true
    override def containsId(id: Int): Boolean = value.id == id

    override def ++(es: UIDSet[T]): UIDSet[T] = {
        if (es eq this)
            return this;

        es.size match {
            case 0 => this
            case 1 => this.incl(es.head)
            case _ => es.incl(value)
        }
    }

    override def compare(that: UIDSet[T]): SetRelation = {
        if (that.isEmpty) {
            StrictSuperset
        } else if (that.isSingletonSet) {
            if (this.value.id == that.head.id) EqualSets else UncomparableSets
        } else if (that.contains(this.value))
            StrictSubset
        else
            UncomparableSets
    }

    override def mapUIDSet[B <: UID](f: T => B): UIDSet[B] =
        UIDSet1(f(value))
}

final class UIDSet2[T <: UID](value1: T, value2: T) extends NonEmptyUIDSet[T] {

    override def size: Int = 2
    override def exists(p: T => Boolean): Boolean = p(value1) || p(value2)
    override def forall(p: T => Boolean): Boolean = p(value1) && p(value2)
    override def foreach[U](f: T => U): Unit = { f(value1); f(value2) }
    override def iterator: Iterator[T] = Iterator(value1, value2)
    override def head: T = value1
    override def last: T = value2
    override def tail: UIDSet[T] = new UIDSet1(value2)
    override def foldLeft[B](z: B)(op: (B, T) => B): B = op(op(z, value1), value2)

    override def find(p: T => Boolean): Option[T] = {
        if (p(value1)) Some(value1) else if (p(value2)) Some(value2) else None

    }

    override def filter(p: T => Boolean): UIDSet[T] = {
        if (p(value1)) {
            if (p(value2))
                this
            else
                new UIDSet1(value1)
        } else if (p(value2)) {
            new UIDSet1(value2)
        } else {
            empty
        }
    }

    override def filterNot(p: T => Boolean): UIDSet[T] = {
        if (p(value1)) {
            if (p(value2))
                empty
            else
                new UIDSet1(value2)
        } else if (p(value2)) {
            new UIDSet1(value1)
        } else {
            this
        }
    }

    override def drop(n: Int): UIDSet[T] = {
        if (n == 0) this else if (n == 1) new UIDSet1(value2) else empty
    }

    override def incl(e: T): UIDSet[T] = {
        val eId = e.id
        val value1 = this.value1
        if (eId == value1.id)
            return this;
        val value2 = this.value2
        if (eId == value2.id)
            return this;

        new UIDSet3(value1, value2, e)
    }

    override def excl(e: T): UIDSet[T] = {
        val eId = e.id
        if (value1.id == eId)
            new UIDSet1(value2)
        else if (value2.id == eId)
            new UIDSet1(value1)
        else
            this
    }

    override def hashCode: Int = value1.id ^ value2.id // ordering independent
    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet[_] =>
                that.size == 2 && {
                    if (that.head.id == value1.id)
                        that.last.id == value2.id
                    else
                        that.head.id == value2.id && that.last.id == value1.id
                }
            case _ => false
        }
    }

    //
    // METHODS DEFINED BY UIDSet
    //

    override def findById(id: Int): Option[T] = {
        if (value1.id == id) Some(value1) else if (value2.id == id) Some(value2) else None
    }

    override def idIterator: IntIterator = IntIterator(value1.id, value2.id)
    override def idSet: IntTrieSet = IntTrieSet.from(value1.id, value2.id)
    override def isSingletonSet: Boolean = false
    override def containsId(id: Int): Boolean = value1.id == id || value2.id == id

    def ++(es: UIDSet[T]): UIDSet[T] = {
        if (es eq this)
            return this;

        es.size match {
            case 0 => this
            case 1 => this.incl(es.head)
            case 2 => this.incl(es.head).incl(es.last)
            case _ => this.foldLeft(es)(_ incl _) // es is larger... which should be less work
        }
    }
}
final object UIDSet2 {
    def apply[T <: UID](value1: T, value2: T): UIDSet2[T] = new UIDSet2[T](value1, value2)
}

final class UIDSet3[T <: UID](value1: T, value2: T, value3: T) extends NonEmptyUIDSet[T] {

    override def size: Int = 3
    override def find(p: T => Boolean): Option[T] = {
        if (p(value1)) Some(value1)
        else if (p(value2)) Some(value2)
        else if (p(value3)) Some(value3)
        else None
    }
    override def exists(p: T => Boolean): Boolean = p(value1) || p(value2) || p(value3)
    override def forall(p: T => Boolean): Boolean = p(value1) && p(value2) && p(value3)
    override def foreach[U](f: T => U): Unit = { f(value1); f(value2); f(value3) }
    override def iterator: Iterator[T] = Iterator(value1, value2, value3)
    override def head: T = value1
    override def last: T = value3
    override def tail: UIDSet[T] = new UIDSet2(value2, value3)
    override def filter(p: T => Boolean): UIDSet[T] = {
        if (p(value1)) {
            if (p(value2)) {
                if (p(value3))
                    this
                else
                    new UIDSet2[T](value1, value2)
            } else {
                if (p(value3))
                    new UIDSet2[T](value1, value3)
                else
                    new UIDSet1[T](value1)
            }
        } else {
            if (p(value2)) {
                if (p(value3))
                    new UIDSet2[T](value2, value3)
                else
                    new UIDSet1[T](value2)
            } else {
                if (p(value3))
                    new UIDSet1[T](value3)
                else
                    empty
            }
        }

    }
    override def filterNot(p: T => Boolean): UIDSet[T] = filter(e => !p(e))
    override def foldLeft[B](z: B)(op: (B, T) => B): B = op(op(op(z, value1), value2), value3)
    override def drop(n: Int): UIDSet[T] = {
        n match {
            case 0 => this
            case 1 => new UIDSet2(value2, value3)
            case 2 => new UIDSet1(value3)
            case _ => empty
        }
    }

    override def incl(e: T): UIDSet[T] = {
        val eId = e.id
        val value1 = this.value1
        if (eId == value1.id)
            return this;
        val value2 = this.value2
        if (eId == value2.id)
            return this;
        val value3 = this.value3
        if (eId == value3.id)
            return this;

        // we only use the trie for sets with more than three elements
        new UIDSetInnerNode(1, value1, null, null) addMutate value2 addMutate value3 addMutate e
    }

    override def excl(e: T): UIDSet[T] = {
        val eId = e.id
        if (value1.id == eId)
            new UIDSet2(value2, value3)
        else if (value2.id == eId)
            new UIDSet2(value1, value3)
        else if (value3.id == eId)
            new UIDSet2(value1, value2)
        else
            this
    }

    override def hashCode: Int = value1.id ^ value2.id ^ value3.id // ordering independent
    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet[_] =>
                (that eq this) || {
                    that.size == 3 &&
                        that.containsId(value1.id) &&
                        that.containsId(value2.id) &&
                        that.containsId(value3.id)
                }
            case _ => false
        }
    }

    //
    // METHODS DEFINED BY UIDSet
    //

    override def findById(id: Int): Option[T] = {
        if (value1.id == id)
            Some(value1)
        else if (value2.id == id)
            Some(value2)
        else if (value3.id == id)
            Some(value3)
        else None
    }

    override def idIterator: IntIterator = IntIterator(value1.id, value2.id, value3.id)
    override def idSet: IntTrieSet = IntTrieSet.from(value1.id, value2.id, value3.id)
    override def isSingletonSet: Boolean = false
    override def containsId(id: Int): Boolean = {
        value1.id == id || value2.id == id || value3.id == id
    }
    override def ++(es: UIDSet[T]): UIDSet[T] = {
        if (es eq this)
            return this;

        es.size match {
            case 0 => this
            case 1 => this.incl(es.head)
            case 2 => this.incl(es.head).incl(es.last)
            case _ => this.foldLeft(es)(_ incl _) // es is at least as large as this set
        }
    }
}

// remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-
//
//
// If we have more than three values we always create a trie.
//
//
// remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-remove-

sealed private[immutable] abstract class UIDSetNodeLike[T <: UID] extends NonEmptyUIDSet[T] {
    self =>

    protected def value: T

    // the following two methods return either a UIDSetNode, a UIDSetLeaf or null:
    protected def left: UIDSetNodeLike[T]
    protected def right: UIDSetNodeLike[T]

    override def find(p: T => Boolean): Option[T] = {
        if (p(value))
            return Some(value);

        if (left ne null) {
            val result = left.find(p);
            if (result.isDefined)
                return result;
        }
        if (right ne null) {
            val result = right.find(p)
            if (result.isDefined)
                return result;
        }
        None
    }

    override def exists(p: T => Boolean): Boolean = {
        p(value) || (left != null && left.exists(p)) || (right != null && right.exists(p))
    }

    override def forall(p: T => Boolean): Boolean = {
        p(value) && {
            val left = this.left
            left == null || left.forall(p)
        } && {
            val right = this.right
            right == null || right.forall(p)
        }
    }

    override def foreach[U](f: T => U): Unit = {
        f(value)
        val left = this.left; if (left ne null) left.foreach(f)
        val right = this.right; if (right ne null) right.foreach(f)
        /*
        val nodes = new Array[UIDSetNodeLike[T]](32 /*IMPROVE... we have to know the depth...*/ )
        nodes(0) = this
        var lastElementOnStack = 0
        while (lastElementOnStack >= 0) {
            val node = nodes(lastElementOnStack)

                    f(node.value)
                    lastElementOnStack remove= 1
                    val left = node.left
                    if (left ne null) {
                        lastElementOnStack add= 1
                        nodes(lastElementOnStack) = left
                    }
                    val right = node.right
                    if (right ne null) {
                        lastElementOnStack add= 1
                        nodes(lastElementOnStack) = right
                    }
        }
*/
    }

    override def iterator: Iterator[T] = new Iterator[T] {
        private[this] val nextNodes = mutable.Stack[UIDSetNodeLike[T]](self)
        def hasNext: Boolean = nextNodes.nonEmpty
        def next(): T = {
            val currentNode = nextNodes.pop()
            val nextRight = currentNode.right
            val nextLeft = currentNode.left
            if (nextRight ne null) nextNodes.push(nextRight)
            if (nextLeft ne null) nextNodes.push(nextLeft)
            currentNode.value
        }
    }

    override def head: T = value

    override def tail: UIDSet[T] = {
        /*current...*/ size match {
            case 1 => empty
            case 2 =>
                val left = this.left
                new UIDSet1(if (left ne null) left.value else right.value)
            case 3 =>
                val left = this.left
                val right = this.right
                if (left eq null)
                    new UIDSet2(right.head, right.last)
                else if (right eq null)
                    new UIDSet2(left.head, left.last)
                else
                    new UIDSet2(left.head, right.head)
            case _ =>
                dropHead
        }
    }

    override def foldLeft[B](z: B)(op: (B, T) => B): B = {
        val left = this.left
        val right = this.right
        var result = op(z, value)
        if (left ne null) result = left.foldLeft(result)(op)
        if (right ne null) result = right.foldLeft(result)(op)
        result
    }

    final def incl(e: T): UIDSet[T] = { val eId = e.id; this.add(e, eId, eId, 0) }

    final def excl(e: T): UIDSet[T] = {
        size match {
            case 1 => throw new UnknownError
            case 2 =>
                val value = this.value
                val eId = e.id
                if (value.id == eId)
                    new UIDSet1(if (left ne null) left.head else right.head)
                else {
                    val value1 = value
                    val value2Candidate = if (left ne null) left.head else right.head
                    if (value2Candidate.id == eId)
                        new UIDSet1(value)
                    else
                        new UIDSet2(value1, value2Candidate)
                }
            case 3 =>
                val value = this.value
                val eId = e.id
                if (value.id == eId) {
                    // let's remove this value
                    if (left ne null) {
                        if (right ne null)
                            new UIDSet2(left.head, right.head)
                        else
                            new UIDSet2(left.head, left.last)
                    } else {
                        new UIDSet2(right.head, right.last)
                    }
                } else {
                    // we have to keep this value...
                    var value2Candidate: T = null.asInstanceOf[T]
                    var value3Candidate: T = null.asInstanceOf[T]
                    if (left ne null) {
                        if (right ne null) {
                            value2Candidate = left.head
                            value3Candidate = right.head
                        } else {
                            value2Candidate = left.head
                            value3Candidate = left.last
                        }
                    } else {
                        value2Candidate = right.head
                        value3Candidate = right.last
                    }
                    if (value2Candidate.id == eId)
                        new UIDSet2(value, value3Candidate)
                    else if (value3Candidate.id == eId)
                        new UIDSet2(value, value2Candidate)
                    else
                        this
                }
            case _ =>
                val eId = e.id
                this.remove(eId, eId)
        }
    }

    override def filter(p: T => Boolean): UIDSet[T] = {
        val result = filter0(p)
        if (result == null)
            return empty;

        result.size match {
            case 1 => new UIDSet1(result.head)
            case 2 => new UIDSet2(result.head, result.last)
            case _ => result
        }
    }

    private def filter0(p: T => Boolean): UIDSetNodeLike[T] = {
        val left = this.left
        val right = this.right
        val newLeft = if (left != null) left.filter0(p) else null
        val newRight = if (right != null) right.filter0(p) else null
        if (p(value)) {
            if ((newLeft ne left) || (newRight ne right)) {
                var newSize = 1
                if (newLeft ne null) newSize += newLeft.size
                if (newRight ne null) newSize += newRight.size
                if (newSize == 1)
                    new UIDSetLeaf(value)
                else
                    new UIDSetInnerNode(newSize, value, newLeft, newRight)
            } else {
                this
            }
        } else {
            selectHead(newLeft, newRight)
        }
    }

    override def filterNot(p: T => Boolean): UIDSet[T] = filter((u: T) => !p(u))

    //
    // METHODS DEFINED BY UIDSet
    //

    override def idIterator: IntIterator = {
        new IntIterator {

            private[this] val nextNodes = mutable.Stack[UIDSetNodeLike[T]](self)

            override def hasNext: Boolean = nextNodes.nonEmpty

            override def next(): Int = {
                val currentNode = nextNodes.pop()
                val nextRight = currentNode.right
                val nextLeft = currentNode.left
                if (nextRight ne null) nextNodes.push(nextRight)
                if (nextLeft ne null) nextNodes.push(nextLeft)
                currentNode.value.id
            }
        }
    }

    override def idSet: IntTrieSet = growIdSet(IntTrieSet.empty)

    protected[immutable] def growIdSet(set: IntTrieSet): IntTrieSet

    override def isSingletonSet: Boolean = false

    override def findById(id: Int): Option[T] = {
        var currentNode: UIDSetNodeLike[T] = this
        var currentShiftedEId = id
        do {
            if (currentNode.value.id == id)
                return Some(currentNode.value);

            if ((currentShiftedEId & 1) == 1)
                currentNode = currentNode.right
            else
                currentNode = currentNode.left

            currentShiftedEId = currentShiftedEId >>> 1

        } while (currentNode ne null)
        None
    }

    override def ++(es: UIDSet[T]): UIDSet[T] = {
        if (es eq this)
            return this;

        es.size match {
            case 0 => this
            case 1 => this.incl(es.head)
            case 2 => this.incl(es.head).incl(es.last)
            case esSize =>
                if (this.size > esSize)
                    es.foldLeft(this: UIDSet[T])(_ incl _)
                else
                    this.foldLeft(es: UIDSet[T])(_ incl _)
        }
    }

    private def selectHead(
        left:  UIDSetNodeLike[T],
        right: UIDSetNodeLike[T]
    ): UIDSetNodeLike[T] = {
        val rightSize = if (right ne null) right.size else 0
        var newSize = rightSize
        if (left ne null) {
            val leftSize = left.size
            newSize += leftSize
            val leftValue = left.head
            if (leftSize == 1) {
                if (right eq null)
                    new UIDSetLeaf(leftValue)
                else
                    new UIDSetInnerNode(newSize, leftValue, null, right)
            } else {
                new UIDSetInnerNode(newSize, leftValue, left.dropHead, right)
            }
        } else if (right ne null) {
            val rightValue = right.head
            if (rightSize == 1) {
                if (left eq null)
                    new UIDSetLeaf(rightValue)
                else
                    new UIDSetInnerNode(newSize, rightValue, left, null)
            } else {
                new UIDSetInnerNode(newSize, rightValue, left, right.dropHead)
            }
        } else {
            null
        }
    }

    private def dropHead: UIDSetNodeLike[T] = {
        if (left ne null) {
            val leftValue = left.head
            if (left.size == 1) {
                if (right eq null)
                    new UIDSetLeaf(leftValue)
                else
                    new UIDSetInnerNode(size - 1, leftValue, null, right)
            } else { //left.size >= 2... but maybe we can pull the right value...
                if ((right ne null) && right.size == 1)
                    new UIDSetInnerNode(size - 1, right.head, left, null)
                else
                    new UIDSetInnerNode(size - 1, leftValue, left.dropHead, right)
            }
        } else if (right ne null) {
            val rightValue = right.head
            if (right.size == 1) {
                if (left eq null)
                    new UIDSetLeaf(rightValue)
                else
                    new UIDSetInnerNode(size - 1, rightValue, left, null)
            } else { //right.size >= 2... but maybe we can pull the left value...
                if ((left ne null) && left.size == 1)
                    new UIDSetInnerNode(size - 1, left.head, null, right)
                else
                    new UIDSetInnerNode(size - 1, rightValue, left, right.dropHead)
            }
        } else {
            null
        }
    }

    override def containsId(id: Int): Boolean = containsId(id, id)

    private[immutable] def containsId(id: Int, shiftedId: Int): Boolean = {
        /* The recursive version is roughly 5% slower...
        this.value.id == eId || {
            if ((shiftedEId & 1) == 1)
                right != null && right.contains(eId, shiftedEId >>> 1)
            else
                left != null && left.contains(eId, shiftedEId >>> 1)
        }
        */

        var currentNode: UIDSetNodeLike[T] = this
        var currentShiftedId = shiftedId
        do {
            if (currentNode.value.id == id)
                return true;

            if ((currentShiftedId & 1) == 1)
                currentNode = currentNode.right
            else
                currentNode = currentNode.left

            currentShiftedId = currentShiftedId >>> 1

        } while (currentNode ne null)
        false
    }

    private[immutable] def addMutate(e: T, eId: Int, shiftedEId: Int, level: Int): UIDSetNodeLike[T]

    private def add(e: T, eId: Int, shiftedEId: Int, level: Int): UIDSetNodeLike[T] = {
        val valueId = this.value.id
        // In the following, we try to minimize the high of the tree.
        if (valueId == eId)
            return this;

        val right = this.right
        val left = this.left
        var newRight = right
        var newLeft = left
        if ((shiftedEId & 1) == 1) {
            // we have to add the value "here" or on the right branch
            if (newRight eq null)
                newRight = new UIDSetLeaf(e)
            else {
                val newShiftedEId = shiftedEId >>> 1
                if ((newLeft eq null) &&
                    (valueId >>> level & 1) == 0 &&
                    !newRight.containsId(eId, newShiftedEId)) {
                    // we can move the current value to the empty left branch...
                    return new UIDSetInnerNode(size + 1, e, new UIDSetLeaf(value), newRight);
                } else {
                    newRight = newRight.add(e, eId, newShiftedEId, level + 1)
                    if (newRight eq right)
                        return this;
                }
            }
        } else {
            if (newLeft eq null)
                newLeft = new UIDSetLeaf(e)
            else {
                val newShiftedEId = shiftedEId >>> 1
                if ((newRight eq null) &&
                    (valueId >>> level & 1) == 1 &&
                    !newLeft.containsId(eId, newShiftedEId)) {
                    // we can move the current value to the empty right branch...
                    return new UIDSetInnerNode(size + 1, e, newLeft, new UIDSetLeaf(value));
                } else {
                    newLeft = newLeft.add(e, eId, newShiftedEId, level + 1)
                    if (newLeft eq left)
                        return this;
                }
            }
        }
        new UIDSetInnerNode(size + 1, value, newLeft, newRight)
    }

    private def remove(eId: Int, shiftedEId: Int): UIDSetNodeLike[T] = {
        // assert( size > 3) // i.e., after removal we still have a tree
        val value = this.value
        if (value.id == eId) {
            dropHead
        } else { // we don't delete this value ...
            val left = this.left
            val right = this.right
            var newLeft = left
            var newRight = right
            if ((shiftedEId & 1) == 1) {
                if (right eq null)
                    return this;
                // we have to search for the value in the right tree
                newRight = right.remove(eId, shiftedEId >>> 1)
                if (newRight eq right)
                    return this;
            } else {
                if (left eq null)
                    return this;
                newLeft = left.remove(eId, shiftedEId >>> 1)
                if (newLeft eq left)
                    return this;
            }
            new UIDSetInnerNode(size - 1, value, newLeft, newRight)
        }
    }

    def showTree(level: Int = 0): String = {
        val indent = "  " * level
        indent + value.id.toBinaryString + s" #$size("+
            (if (left ne null) s"\n$indent left ="+left.showTree(level + 1)+"\n" else "") +
            (if (right ne null) s"\n$indent right="+right.showTree(level + 1)+"\n)" else ")")
    }
}

private[immutable] object UIDSetNode {

    def apply[T <: UID](
        size:  Int,
        value: T,
        left:  UIDSetNodeLike[T],
        right: UIDSetNodeLike[T]
    ): UIDSetNodeLike[T] = {
        if (size == 1)
            new UIDSetLeaf(value)
        else
            new UIDSetInnerNode(size, value, left, right)
    }

}

final class UIDSetLeaf[T <: UID] private[immutable] (
        val value: T
) extends UIDSetNodeLike[T] {
    override def size: Int = 1
    override def left: UIDSetNodeLike[T] = null
    override def right: UIDSetNodeLike[T] = null
    override def head: T = value
    override def tail: UIDSet[T] = empty
    override def last: T = value
    override def filter(p: T => Boolean): UIDSet[T] = if (p(value)) this else null
    override def foldLeft[B](z: B)(op: (B, T) => B): B = op(z, value)
    override def exists(p: T => Boolean): Boolean = p(value)
    override def forall(p: T => Boolean): Boolean = p(value)
    override def foreach[U](f: T => U): Unit = f(value)
    override def iterator: Iterator[T] = Iterator(value)
    override def find(p: T => Boolean): Option[T] = if (p(value)) Some(value) else None
    override def findById(id: Int): Option[T] = if (value.id == id) Some(value) else None

    override def hashCode: Int = value.id.hashCode()

    override def equals(that: Any): Boolean = {
        that match {
            case that: UIDSet[_] => that.size == 1 && that.head.id == this.value.id
            case _               => false
        }
    }

    override private[opalj] def addMutate(e: T): UIDSet[T] = throw new UnknownError

    private[immutable] def addMutate(e: T, eId: Int, shiftedEId: Int, level: Int): UIDSetNodeLike[T] = {
        if (value.id == eId)
            return this;

        if ((shiftedEId & 1) == 1)
            new UIDSetInnerNode(2, value, null, new UIDSetLeaf(e))
        else
            new UIDSetInnerNode(2, value, new UIDSetLeaf(e), null)
    }

    override def containsId(id: Int): Boolean = id == value.id
    protected[immutable] def growIdSet(set: IntTrieSet): IntTrieSet = set + value.id

}

// we wan't to be able to adapt the case class...
final class UIDSetInnerNode[T <: UID] private[immutable] (
        protected var theSize:          Int,
        protected[immutable] var value: T,
        protected[immutable] var left:  UIDSetNodeLike[T],
        protected[immutable] var right: UIDSetNodeLike[T]
) extends UIDSetNodeLike[T] {

    override def size: Int = theSize

    override def last: T = {
        if (right ne null)
            right.last
        else if (left ne null)
            left.last
        else
            value
    }

    override def hashCode: Int = {
        // ordering independent
        var hash = value.id
        if (left ne null) hash ^= left.hashCode()
        if (right ne null) hash ^= right.hashCode()
        hash
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: UIDSet[_] =>
                (that eq this) ||
                    (that.size == theSize && this.forall(e => that.containsId(e.id)))
            case _ => false
        }
    }

    override private[opalj] def addMutate(e: T): UIDSet[T] = {
        val eId = e.id
        this.addMutate(e, eId, eId, 0)
        this
    }

    private[immutable] def addMutate(
        e:          T,
        eId:        Int,
        shiftedEId: Int,
        level:      Int
    ): UIDSetNodeLike[T] = {
        val value = this.value
        val valueId = value.id
        if (eId == valueId)
            return this;

        val left = this.left
        val right = this.right
        if ((shiftedEId & 1) == 1) {
            // we have to add the new value here or to the right branch
            if (right eq null) {
                this.right = new UIDSetLeaf(e)
                this.theSize += 1
            } else if ((left eq null) &&
                (valueId >>> level & 1) == 0 &&
                !right.containsId(eId, shiftedEId >>> 1)) {
                this.left = new UIDSetLeaf(value)
                this.value = e
                this.theSize += 1
            } else {
                val newRight = right.addMutate(e, eId, shiftedEId >>> 1, level + 1)
                this.right = newRight
                this.theSize = (if (left ne null) left.size else 0) + newRight.size + 1
            }
        } else {
            // we have to add the new value here or to the left branch
            if (left eq null) {
                this.left = new UIDSetLeaf(e)
                this.theSize += 1
            } else if ((right eq null) &&
                (valueId >>> level & 1) == 1 &&
                !left.containsId(eId, shiftedEId >>> 1)) {
                this.right = new UIDSetLeaf(value)
                this.value = e
                this.theSize += 1
            } else {
                val newLeft = left.addMutate(e, eId, shiftedEId >>> 1, level + 1)
                this.left = newLeft
                this.theSize = newLeft.size + (if (right ne null) right.size else 0) + 1
            }
        }
        this
    }

    protected[immutable] def growIdSet(set: IntTrieSet): IntTrieSet = {
        var newSet = set + value.id
        val nextLeft = this.left
        if (nextLeft ne null) {
            newSet = nextLeft.growIdSet(newSet)
        }
        val nextRight = this.right
        if (nextRight ne null) {
            newSet = nextRight.growIdSet(newSet)
        }
        newSet
    }
}

object UIDSet {

    class UIDSetBuilder[T <: UID](var set: UIDSet[T] = empty[T]) extends Builder[T, UIDSet[T]] {
        override def addOne(elem: T): this.type = {
            set = set.addMutate(elem)
            this
        }
        override def clear(): Unit = set = empty
        override def result(): UIDSet[T] = set
    }

    def newBuilder[T <: UID]: UIDSetBuilder[T] = new UIDSetBuilder[T]

    def empty[T <: UID]: UIDSet[T] = UIDSet0.asInstanceOf[UIDSet[T]]

    def fromSpecific[T <: UID](it: IterableOnce[T]): UIDSet[T] = {
        val builder = newBuilder[T]
        val iterator = it.iterator
        while (iterator.hasNext)
            builder.addOne(iterator.next())
        builder.result()
    }

    def apply[T <: UID](vs: T*): UIDSet[T] = {
        if (vs.isEmpty)
            empty[T]
        else {
            vs.foldLeft(empty[T]: UIDSet[T])(_ addMutate _)
        }
    }

}
