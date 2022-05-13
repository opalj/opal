/* BSD 2-Clause License - see OPAL/LICENSE for details. */

package org.opalj.collection
package immutable

import scala.collection.mutable

/**
 * A set of objects of type UID. This set is defined over the ids of the objects and
 * NOT over the objects themselves. I.e., at any given time no two different objects
 * which have the same id, will be found in the set (provided that the ids are not changed
 * after adding the object to this set, which is a pre-requisite.)
 *
 * @note Though `equals` and `hashCode` are implemented, comparing UID trie sets
 *       is still not efficient (n * log n) because the structure of the trie
 *       depends on the insertion order.
 */
sealed abstract class UIDTrieSet[T <: UID] { set =>

    def isEmpty: Boolean
    def nonEmpty: Boolean
    def isSingletonSet: Boolean
    def size: Int
    /**
     * Tests if this set contains a value with the same id as the given value.
     * I.e., no comparison of the values is done, but only the underlying ids
     * are compared.
     */
    final def contains(value: T): Boolean = containsId(value.id)
    def containsId(id: Int): Boolean
    def foreach[U](f: T => U): Unit
    def forall(p: T => Boolean): Boolean
    def exists(p: T => Boolean): Boolean
    def foldLeft[B](z: B)(op: (B, T) => B): B
    final def foreachIterator: ForeachRefIterator[T] = new ForeachRefIterator[T] {
        override def foreach[U](f: T => U): Unit = set.foreach(f)
    }
    def iterator: Iterator[T]
    def add(value: T): UIDTrieSet[T]
    def head: T

    // TODO: optimize implementation
    def ++(other: UIDTrieSet[T]): UIDTrieSet[T] = other.foldLeft(this)((r, e) => r add e)

    final override def equals(other: Any): Boolean = {
        other match {
            case that: UIDTrieSet[_] => this.equals(that)
            case _                   => false
        }
    }

    def equals(other: UIDTrieSet[_]): Boolean
}

/**
 * Factory methods for creating UIDTrieSets.
 */
object UIDTrieSet {

    def empty[T <: UID]: UIDTrieSet[T] = {
        UIDTrieSet0.asInstanceOf[UIDTrieSet[T]]
    }

    def apply[T <: UID](value: T): UIDTrieSet[T] = {
        new UIDTrieSet1(value)
    }

    def apply[T <: UID](value1: T, value2: T): UIDTrieSet[T] = {
        if (value1.id == value2.id)
            new UIDTrieSet1(value1)
        else
            new UIDTrieSet2(value1, value2)
    }
}

/**
 * The common superclass of the nodes of the trie.
 */
private[immutable] sealed trait UIDTrieSetNode[T <: UID] {

    def foreach[U](f: T => U): Unit
    def foldLeft[B](z: B)(op: (B, T) => B): B
    def forall(p: T => Boolean): Boolean
    def exists(p: T => Boolean): Boolean
    def head: T

    //
    // IMPLEMENTATION "INTERNAL" METHODS
    //
    // Adds the value with the id to the trie at the specified level where `key is id >> level`.
    private[immutable] def add(value: T, id: Int, key: Int, level: Int): UIDTrieSetNode[T]
    private[immutable] def containsId(id: Int, key: Int): Boolean
    private[immutable] def toString(indent: Int): String
}

/**
 * The common superclass of the leafs of the trie.
 */
private[immutable] sealed abstract class UIDTrieSetLeaf[T <: UID]
    extends UIDTrieSet[T]
    with UIDTrieSetNode[T] {

    final override private[immutable] def containsId(id: Int, key: Int): Boolean = {
        this.containsId(id)
    }

    final override private[immutable] def toString(indent: Int): String = {
        (" " * indent) + toString()
    }

}

case object UIDTrieSet0 extends UIDTrieSetLeaf[UID] {
    override def isSingletonSet: Boolean = false
    override def isEmpty: Boolean = true
    override def nonEmpty: Boolean = false
    override def size: Int = 0
    override def foreach[U](f: UID => U): Unit = {}
    override def forall(p: UID => Boolean): Boolean = true
    override def exists(p: UID => Boolean): Boolean = false
    override def foldLeft[B](z: B)(op: (B, UID) => B): B = z
    override def add(i: UID): UIDTrieSet1[UID] = new UIDTrieSet1(i)
    override def iterator: Iterator[UID] = Iterator.empty
    override def containsId(id: Int): Boolean = false
    override def head: UID = throw new NoSuchElementException

    override def equals(other: UIDTrieSet[_]): Boolean = other eq this
    override def hashCode: Int = 0
    override def toString: String = "UIDTrieSet()"

    override private[immutable] def add(
        value: UID,
        id:    Int,
        key:   Int,
        level: Int
    ): UIDTrieSetNode[UID] = {
        this add value
    }
}

final class UIDTrieSet1[T <: UID](val i: T) extends UIDTrieSetLeaf[T] {
    override def isEmpty: Boolean = false
    override def nonEmpty: Boolean = true
    override def isSingletonSet: Boolean = true
    override def size: Int = 1
    override def foreach[U](f: T => U): Unit = { f(i) }
    override def forall(p: T => Boolean): Boolean = p(i)
    override def exists(p: T => Boolean): Boolean = p(i)
    override def foldLeft[B](z: B)(op: (B, T) => B): B = op(z, i)
    override def add(value: T): UIDTrieSetLeaf[T] = {
        val v = this.i
        if (v.id == value.id) this else new UIDTrieSet2(v, value)
    }
    override def iterator: Iterator[T] = Iterator(i)
    override def containsId(id: Int): Boolean = id == i.id
    override def head: T = i

    override def equals(other: UIDTrieSet[_]): Boolean = {
        (other eq this) || (other match {
            case that: UIDTrieSet1[_] => this.i.id == that.i.id
            case that                 => false
        })
    }

    override def hashCode: Int = i.id

    override def toString: String = s"UIDTrieSet($i)"

    override private[immutable] def add(
        value: T,
        id:    Int,
        key:   Int,
        level: Int
    ): UIDTrieSetNode[T] = {
        this add value
    }
}

/**
 * Represents an ordered set of two values where i1 has to be smaller than i2.
 */
private[immutable] final class UIDTrieSet2[T <: UID] private[immutable] (
        val i1: T, val i2: T
) extends UIDTrieSetLeaf[T] {

    override def isEmpty: Boolean = false
    override def nonEmpty: Boolean = true
    override def isSingletonSet: Boolean = false
    override def size: Int = 2
    override def iterator: Iterator[T] = Iterator(i1, i2)
    override def foreach[U](f: T => U): Unit = { f(i1); f(i2) }
    override def forall(p: T => Boolean): Boolean = { p(i1) && p(i2) }
    override def exists(p: T => Boolean): Boolean = { p(i1) || p(i2) }
    override def foldLeft[B](z: B)(op: (B, T) => B): B = op(op(z, i1), i2)
    override def containsId(id: Int): Boolean = id == i1.id || id == i2.id
    override def head: T = i1
    override def add(value: T): UIDTrieSetLeaf[T] = {
        val id = value.id

        val i1 = this.i1
        if (i1.id == id) {
            return this;
        }

        val i2 = this.i2
        if (i2.id == id) {
            this
        } else {
            new UIDTrieSet3(i1, i2, value)
        }
    }

    override def equals(other: UIDTrieSet[_]): Boolean = {
        (other eq this) || (
            other match {
                case that: UIDTrieSet2[_] =>
                    (this.i1.id == that.i1.id && this.i2.id == that.i2.id) ||
                        (this.i1.id == that.i2.id && this.i2.id == that.i1.id)
                case that =>
                    false
            }
        )
    }

    override def hashCode: Int = i1.id ^ i2.id // ordering independent

    override def toString: String = s"UIDTrieSet($i1, $i2)"

    override private[immutable] def add(
        value: T,
        id:    Int,
        key:   Int,
        level: Int
    ): UIDTrieSetNode[T] = {
        this add value
    }

}

/**
 * Represents an ordered set of three int values: i1 < i2 < i3.
 */
private[immutable] final class UIDTrieSet3[T <: UID] private[immutable] (
        val i1: T, val i2: T, val i3: T
) extends UIDTrieSetLeaf[T] {

    override def size: Int = 3
    override def isEmpty: Boolean = false
    override def nonEmpty: Boolean = true
    override def isSingletonSet: Boolean = false
    override def containsId(id: Int): Boolean = id == i1.id || id == i2.id || id == i3.id
    override def head: T = i1
    override def iterator: Iterator[T] = Iterator(i1, i2, i3)
    override def foreach[U](f: T => U): Unit = { f(i1); f(i2); f(i3) }
    override def forall(p: T => Boolean): Boolean = { p(i1) && p(i2) && p(i3) }
    override def exists(p: T => Boolean): Boolean = { p(i1) || p(i2) || p(i3) }
    override def foldLeft[B](z: B)(op: (B, T) => B): B = op(op(op(z, i1), i2), i3)

    override def add(value: T): UIDTrieSet[T] = {
        val id = value.id
        val newSet = this.add(value, id, id, 0)
        if (newSet ne this)
            new UIDTrieSetN(4, newSet)
        else
            this
    }

    override def equals(other: UIDTrieSet[_]): Boolean = {
        (other eq this) || (
            other match {
                case that: UIDTrieSet3[_] =>
                    that.containsId(this.i1.id) &&
                        that.containsId(this.i2.id) &&
                        that.containsId(this.i3.id)
                case that =>
                    false
            }
        )
    }

    override def hashCode: Int = i1.id ^ i2.id ^ i3.id // ordering independent

    override def toString: String = s"UIDTrieSet($i1, $i2, $i3)"

    override private[immutable] def add(
        value: T,
        id:    Int,
        key:   Int,
        level: Int
    ): UIDTrieSetNode[T] = {
        val i1 = this.i1
        val i1Id = i1.id
        if (id == i1Id)
            return this;

        val i2 = this.i2
        val i2Id = i2.id
        if (id == i2Id)
            return this;

        val i3 = this.i3
        val i3Id = i3.id
        if (id == i3Id)
            return this;

        if ((key & 1) == 0) {
            if ((i1Id >> level & 1) == 0) {
                if ((i2Id >> level & 1) == 0) {
                    if ((i3Id >> level & 1) == 0) {
                        new UIDTrieSetNode_0(value, this)
                    } else {
                        new UIDTrieSetNode_0(
                            i3,
                            new UIDTrieSet3(value, i1, i2)
                        )
                    }
                } else {
                    if ((i3Id >> level & 1) == 0) {
                        new UIDTrieSetNode_0(
                            i2,
                            new UIDTrieSet3(value, i1, i3)
                        )
                    } else {
                        new UIDTrieSetNode_0_1(
                            value,
                            new UIDTrieSet1(i1),
                            new UIDTrieSet2(i2, i3)
                        )
                    }
                }
            } else {
                // value >  _0, i1 =>  _1, ...
                if ((i2Id >> level & 1) == 0) {
                    if ((i3Id >> level & 1) == 0) {
                        new UIDTrieSetNode_0(
                            i1,
                            new UIDTrieSet3(value, i2, i3)
                        )
                    } else {
                        // value >  _0, i1 =>  _1, i2 => _ 0, i3 => _1
                        new UIDTrieSetNode_0_1(
                            value,
                            new UIDTrieSet1(i2),
                            new UIDTrieSet2(i1, i3)
                        )
                    }
                } else {
                    // value >  _0, i1 =>  _1, i2 => _ 1, ...
                    if ((i3Id >> level & 1) == 0) {
                        new UIDTrieSetNode_0_1(
                            value,
                            new UIDTrieSet1(i3),
                            new UIDTrieSet2(i1, i2)
                        )
                    } else {
                        new UIDTrieSetNode_1(value, this)
                    }
                }
            }
        } else {
            // value =>  _1,  ...
            if ((i1Id >> level & 1) == 0) {
                if ((i2Id >> level & 1) == 0) {
                    if ((i3Id >> level & 1) == 0) {
                        new UIDTrieSetNode_0(value, this)
                    } else {
                        new UIDTrieSetNode_0_1(
                            value,
                            new UIDTrieSet2(i1, i2),
                            new UIDTrieSet1(i3)
                        )
                    }
                } else {
                    // value =>  _1, i1 => 0,  i2 => _1
                    if ((i3Id >> level & 1) == 0) {
                        new UIDTrieSetNode_0_1(
                            value,
                            new UIDTrieSet2(i1, i3),
                            new UIDTrieSet1(i2)
                        )
                    } else {
                        new UIDTrieSetNode_1(
                            i1,
                            new UIDTrieSet3(value, i2, i3)
                        )
                    }
                }
            } else {
                // value =>  _1, i1 =>  _1, ...
                if ((i2Id >> level & 1) == 0) {
                    if ((i3Id >> level & 1) == 0) {
                        new UIDTrieSetNode_0_1(
                            value,
                            new UIDTrieSet2(i2, i3),
                            new UIDTrieSet1(i1)
                        )
                    } else {
                        // value =>  _1, i1 =>  _1, i2 => _ 0, i3 => _1
                        new UIDTrieSetNode_1(
                            i2,
                            new UIDTrieSet3(value, i1, i3)
                        )
                    }
                } else {
                    // value =>  _1, i1 =>  _1, i2 => _ 1, ...
                    if ((i3Id >> level & 1) == 0) {
                        new UIDTrieSetNode_1(
                            i3,
                            new UIDTrieSet3(value, i1, i2)
                        )
                    } else {
                        new UIDTrieSetNode_1(value, this)
                    }
                }
            }
        }
    }
}

/**
 * A UIDTrieSet with four or more values.
 */
private[immutable] final class UIDTrieSetN[T <: UID](
        val size: Int,
        root:     UIDTrieSetNode[T]
) extends UIDTrieSet[T] {

    // assert(size >= 4)

    override def isEmpty: Boolean = false
    override def nonEmpty: Boolean = true
    override def isSingletonSet: Boolean = false
    override def containsId(id: Int): Boolean = root.containsId(id, id)
    override def head: T = root.head
    override def foreach[U](f: T => U): Unit = root.foreach(f)
    override def forall(p: T => Boolean): Boolean = root.forall(p)
    override def exists(p: T => Boolean): Boolean = root.exists(p)
    override def foldLeft[B](z: B)(op: (B, T) => B): B = root.foldLeft(z)(op)

    override def iterator: Iterator[T] = new Iterator[T] {
        private[this] var currentNode = root
        private[this] var index = 0
        private[this] val furtherNodes = mutable.Stack.empty[UIDTrieSetNode[T]]
        def hasNext: Boolean = currentNode ne null
        def next(): T = {
            (this.currentNode: @unchecked) match {
                case n: UIDTrieSet1[T] =>
                    this.currentNode = if (furtherNodes.nonEmpty) furtherNodes.pop() else null
                    n.i
                case n: UIDTrieSet2[T] =>
                    if (index == 0) {
                        index = 1
                        n.i1
                    } else {
                        this.currentNode = if (furtherNodes.nonEmpty) furtherNodes.pop() else null
                        index = 0
                        n.i2
                    }
                case n: UIDTrieSet3[T] =>
                    if (index == 0) {
                        index = 1
                        n.i1
                    } else if (index == 1) {
                        index = 2
                        n.i2
                    } else {
                        this.currentNode = if (furtherNodes.nonEmpty) furtherNodes.pop() else null
                        index = 0
                        n.i3
                    }
                case n: UIDTrieSetNode_0[T] =>
                    currentNode = n._0
                    n.v
                case n: UIDTrieSetNode_1[T] =>
                    currentNode = n._1
                    n.v
                case n: UIDTrieSetNode_0_1[T] =>
                    currentNode = n._0
                    furtherNodes.push(n._1)
                    n.v
            }
        }
    }

    override def equals(that: UIDTrieSet[_]): Boolean = {
        (that eq this) ||
            // recall that the shape of the trie depends on the insertion order
            // (but doesn't reflect the order)
            (that.size == this.size && this.forall(uid => that.containsId(uid.id)))
    }

    override def hashCode: Int = root.hashCode * size

    override def add(value: T): UIDTrieSet[T] = {
        val id = value.id
        val root = this.root
        val newRoot = root.add(value, id, id, 0)
        if (newRoot ne root) {
            new UIDTrieSetN(size + 1, newRoot)
        } else {
            this
        }
    }

    override def toString: String = s"UIDTrieSet(#$size, ${root.toString(1)})"

}

private[immutable] final class UIDTrieSetNode_0_1[T <: UID](
        val v:  T, // value with the current prefix...
        val _0: UIDTrieSetNode[T],
        val _1: UIDTrieSetNode[T]
) extends UIDTrieSetNode[T] {

    override def foreach[U](f: T => U): Unit = { f(v); _0.foreach(f); _1.foreach(f) }
    override def foldLeft[B](z: B)(op: (B, T) => B): B = _1.foldLeft(_0.foldLeft(op(z, v))(op))(op)
    override def forall(p: T => Boolean): Boolean = { p(v) && _0.forall(p) && _1.forall(p) }
    override def exists(p: T => Boolean): Boolean = { p(v) || _0.forall(p) || _1.forall(p) }
    override def head: T = v

    override def hashCode: Int = v.id ^ _0.hashCode ^ _1.hashCode

    override private[immutable] def add(
        value: T,
        id:    Int,
        key:   Int,
        level: Int
    ): UIDTrieSetNode[T] = {
        val v = this.v
        if (v.id != id) {
            if ((key & 1) == 0) {
                val new_0 = _0.add(value, id, key >> 1, level + 1)
                if (new_0 ne _0) {
                    new UIDTrieSetNode_0_1(v, new_0, _1)
                } else {
                    this
                }
            } else {
                val new_1 = _1.add(value, id, key >> 1, level + 1)
                if (new_1 ne _1) {
                    new UIDTrieSetNode_0_1(v, _0, new_1)
                } else {
                    this
                }
            }
        } else {
            this
        }
    }

    override private[immutable] def containsId(id: Int, key: Int): Boolean = {
        this.v.id == id || {
            val newKey = key >> 1
            if ((key & 1) == 0) _0.containsId(id, newKey) else _1.containsId(id, newKey)
        }
    }

    override private[immutable] def toString(indent: Int): String = {
        val spaces = " " * indent
        s"N($v,\n${spaces}0=>${_0.toString(indent + 1)},\n${spaces}1=>${_1.toString(indent + 1)})"
    }

}

private[immutable] final class UIDTrieSetNode_0[T <: UID](
        val v:  T,
        val _0: UIDTrieSetNode[T]
) extends UIDTrieSetNode[T] {

    override def hashCode: Int = v.id ^ _0.hashCode

    override def foreach[U](f: T => U): Unit = { f(v); _0.foreach(f) }
    override def foldLeft[B](z: B)(op: (B, T) => B): B = _0.foldLeft(op(z, v))(op)
    override def forall(p: T => Boolean): Boolean = { p(v) && _0.forall(p) }
    override def exists(p: T => Boolean): Boolean = { p(v) || _0.forall(p) }
    override def head: T = v

    override private[immutable] def add(
        value: T,
        id:    Int,
        key:   Int,
        level: Int
    ): UIDTrieSetNode[T] = {
        val v = this.v
        val vId = v.id
        if (vId != id) {
            if ((key & 1) == 0) {
                // let's check if we can improve the balancing of the tree by putting
                // the new value in this node and moving this node further down the tree...
                if (((vId >> level) & 1) == 1) {
                    if (!_0.containsId(id, key >> 1)) {
                        new UIDTrieSetNode_0_1(value, _0, new UIDTrieSet1(v))
                    } else {
                        this
                    }
                } else {
                    val new_0 = _0.add(value, id, key >> 1, level + 1)
                    if (new_0 ne _0) {
                        new UIDTrieSetNode_0(v, new_0)
                    } else {
                        this
                    }
                }
            } else {
                val new_1 = new UIDTrieSet1(value)
                new UIDTrieSetNode_0_1(v, _0, new_1)
            }
        } else {
            this
        }
    }

    private[immutable] def containsId(id: Int, key: Int): Boolean = {
        this.v.id == id || ((key & 1) == 0 && _0.containsId(id, key >> 1))
    }

    private[immutable] def toString(indent: Int): String = {
        val spaces = " " * indent
        s"N($v,\n${spaces}0=>${_0.toString(indent + 1)})"
    }
}

private[immutable] final class UIDTrieSetNode_1[T <: UID](
        val v:  T,
        val _1: UIDTrieSetNode[T]
) extends UIDTrieSetNode[T] {

    override def hashCode: Int = v.id ^ _1.hashCode

    override def foreach[U](f: T => U): Unit = { f(v); _1.foreach(f) }
    override def foldLeft[B](z: B)(op: (B, T) => B): B = _1.foldLeft(op(z, v))(op)
    override def forall(p: T => Boolean): Boolean = { p(v) && _1.forall(p) }
    override def exists(p: T => Boolean): Boolean = { p(v) || _1.forall(p) }
    override def head: T = v

    override private[immutable] def add(
        value: T,
        id:    Int,
        key:   Int,
        level: Int
    ): UIDTrieSetNode[T] = {
        val v = this.v
        val vId = v.id
        if (vId != id) {
            if ((key & 1) == 0) {
                val new_0 = new UIDTrieSet1(value)
                new UIDTrieSetNode_0_1(v, new_0, _1)
            } else {
                // let's check if we can improve the balancing of the tree by putting
                // the new value in this node and moving this node further down the tree...
                if (((vId >> level) & 1) == 0) {
                    if (!_1.containsId(id, key >> 1)) {
                        new UIDTrieSetNode_0_1(value, new UIDTrieSet1(v), _1)
                    } else {
                        this
                    }
                } else {
                    val new_1 = _1.add(value, id, key >> 1, level + 1)
                    if (new_1 ne _1) {
                        new UIDTrieSetNode_1(v, new_1)
                    } else {
                        this
                    }
                }
            }
        } else {
            this
        }
    }

    override private[immutable] def containsId(id: Int, key: Int): Boolean = {
        this.v.id == id || ((key & 1) == 1 && _1.containsId(id, key >> 1))
    }

    override private[immutable] def toString(indent: Int): String = {
        val spaces = " " * indent
        s"N($v,\n${spaces}1=>${_1.toString(indent + 1)})"
    }

}