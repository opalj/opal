/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

import scala.annotation.switch

import java.lang.{Long ⇒ JLong}
import java.lang.Long.{hashCode ⇒ lHashCode}
import java.lang.Math.abs

/**
 * An effectively immutable trie set of long values where the elements are not sorted.
 *
 * @author Michael Eichberg
 */
sealed abstract class LongTrieSet {
    def size: Int
    def isEmpty: Boolean
    def nonEmpty: Boolean
    def isSingletonSet: Boolean
    def contains(v: Long): Boolean
    def foreach[U](f: Long ⇒ U): Unit
   // def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T
   // def iterator: LongIterator
    def +(v: Long): LongTrieSet
}

object LongTrieSet {

    def empty: LongTrieSet = EmptyLongTrieSet

    def apply(v: Long): LongTrieSet = new LongTrieSet1(v)

    def apply(v1: Long, v2: Long): LongTrieSet = {
        if (v1 == v2)
            new LongTrieSet1(v1)
        else
            new LongTrieSet2(v1, v2)
    }

    def apply(v1: Long, v2: Long,v3 : Long): LongTrieSet = {
        if (v1 == v2) {
          if(v2 == v3) {
            new LongTrieSet1(v1)
          } else {
            new LongTrieSet2(v1,v2)
          }
        } else if (v2 == v3){
            new LongTrieSet2(v1,v2)
        } else if (v1 == v3) {
            new LongTrieSet2(v1,v2)
        } else {
            new LongTrieSet3(v1, v2,v3)
        }
    }

    def apply(v1: Long, v2: Long,v3 : Long, v4 : Long): LongTrieSet = {
        apply(v1,v2,v3) + v4
    }
}

case object EmptyLongTrieSet extends LongTrieSet {
    final override def size: Int = 0
    final override def isEmpty: Boolean = true
    final override def nonEmpty: Boolean = false
    final override def isSingletonSet: Boolean = false
    final override def contains(v: Long): Boolean = false
    final override def foreach[U](f: Long ⇒ U): Unit = { /*nothing to do*/ }
    def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = op
    def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = { /*nothing to do*/ }
    def head: Long = throw new UnsupportedOperationException
    def iterator: LongIterator = LongIterator.empty
    final override def +(v: Long): LongTrieSet = LongTrieSet1(v)
}

final case class LongTrieSet1(v1: Long) extends LongTrieSet {
    final override def size: Int = 1
    final override def isEmpty: Boolean = false
    final override def nonEmpty: Boolean = true
    final override def isSingletonSet: Boolean = true
    final override def contains(v: Long): Boolean = v == v1
    final override def foreach[U](f: Long ⇒ U): Unit = f(v1)
    def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = f(op, v1)
    def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = if (n > 0) f(v1)
    def head: Long = v1
    def iterator: LongIterator = LongIterator(v1)
    final override def +(v: Long): LongTrieSet = {
        if (v != v1) new LongTrieSet2(v, v1) else this
    }
}

final private[immutable] case class LongTrieSet2(v1: Long, v2: Long) extends LongTrieSet {
    final override def size: Int = 2
    final override def isEmpty: Boolean = false
    final override def nonEmpty: Boolean = true
    final override def isSingletonSet: Boolean = false
    final override def contains(v: Long): Boolean = v == v1 || v == v2
    final override def foreach[U](f: Long ⇒ U): Unit = { f(v1); f(v2) }
    def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = f(f(op, v1), v2)
    def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
        if (n > 0) f(v1)
        if (n > 1) f(v2)
    }
    def head: Long = v1
     def iterator: LongIterator = LongIterator(v1, v2)
    final override def +(v: Long): LongTrieSet = {
        if (v != v1 && v != v2) new LongTrieSet3(v, v1, v2) else this
    }
}

final private[immutable] case class LongTrieSet3(v1: Long, v2: Long, v3: Long) extends LongTrieSet {
    final override def size: Int = 3
    final override def isEmpty: Boolean = false
    final override def nonEmpty: Boolean = true
    final override def isSingletonSet: Boolean = false
    final override def contains(v: Long): Boolean = v == v1 || v == v2 || v == v3
    final override def foreach[U](f: Long ⇒ U): Unit = { f(v1); f(v2); f(v3) }
    def foldLeft[T](op: T)(f: (T, Long) ⇒ T): T = f(f(f(op, v1), v2), v3)
    def forFirstN[U](n: Int)(f: Long ⇒ U): Unit = {
        if (n > 0) f(v1)
        if (n > 1) f(v2)
        if (n > 2) f(v3)
    }
    def head: Long = v1
    def iterator: LongIterator = LongIterator(v1, v2, v3)
    final override def +(v: Long): LongTrieSet = {
        if (v != v1 && v != v2 && v != v3) {
            val set = new LargeLongTrieSet()
            set += v3
            set += v2
            set += v1
            set += v
            set
        } else {
            this
        }
    }
}

/** The super type of the nodes of the trie set. */
private[immutable] abstract class LongTrieSetNode {

    /** `true` if this is an inner node. */
    def isN: Boolean
    def isN4: Boolean
    /** `true` if this is a leaf node. */
    def isL: Boolean
    def asL: LongTrieSetL

    def foreach[U](f: Long ⇒ U): Unit

    def +(v: Long, level: Int): LongTrieSetNode

    def toString(level: Int): String

    /** Returns the node for the path which has another "0" bit. */
    def split_0(level: Int): LongTrieSetNode

    /** Returns the node for the path which has another "1" bit. */
    def split_1(level: Int): LongTrieSetNode

}


private[immutable] abstract class LongTrieSetL  extends LongTrieSetNode  {

    final override def isN: Boolean = false
    final override def isN4: Boolean = false
    final override def isL: Boolean = true
    final override def asL: LongTrieSetL = this

    def size : Int
    def contains( v : Long ): Boolean

}

/** The leaves of the trie set. */
final private[immutable] case class LongTrieSetL1(
        v1: Long
) extends LongTrieSetL {

    override def size : Int= 1
    override def contains( v : Long ): Boolean  = v == v1

    override def foreach[U](f: Long ⇒ U): Unit = f(v1)

    override    def +(v: Long, level: Int): LongTrieSetNode = {
        if( v != v1) new LongTrieSetL2(v,v1) else this
    }

    override    def split_0(level: Int): LongTrieSetNode = {
        if ((((v1 >> level) & 1L) == 0L)) {
            this
        } else {
            null
        }
    }

    override     def split_1(level: Int): LongTrieSetNode = {
        if ((((v1 >> level) & 1L) == 1L)) {
            this
        } else {
            null
        }
    }

    override def toString(level: Int): String = s"L1(${v1.toBinaryString}=$v1)"
}

final private[immutable] case class LongTrieSetL2(
                                                     v1: Long,
                                                     v2: Long
                                                 ) extends LongTrieSetL {

    override def size : Int= 2

    override def contains( v : Long ): Boolean  = v == v1 ||v == v2

    override def foreach[U](f: Long ⇒ U): Unit = {f(v1); f(v2)}

    override    def +(v: Long, level: Int): LongTrieSetNode = {
        if( v != v1 && v != v2)
            new LongTrieSetL3(v,v1,v2)
        else
            this
    }

    override    def split_0(level: Int): LongTrieSetNode = {
        if ((((v1 >> level) & 1L) == 0L)) {
            if ((((v2 >> level) & 1L) == 0L)) {
                this
            } else {
             new LongTrieSetL1(v1)
            }
        } else {
            if ((((v2 >> level) & 1L) == 0L)) {
                new LongTrieSetL1(v2)
            } else {
                null
            }
        }
    }

    override     def split_1(level: Int): LongTrieSetNode = {
        if ((((v1 >> level) & 1L) == 0L)) {
            if ((((v2 >> level) & 1L) == 0L)) {
                null
            } else {
                new LongTrieSetL1(v2)
            }
        } else {
            if ((((v2 >> level) & 1L) == 0L)) {
                new LongTrieSetL1(v1)
            } else {
                this
            }
        }
    }

    override def toString(level: Int): String = {
        s"L2(${v1.toBinaryString}=$v1, ${v2.toBinaryString}=$v2)"
    }
}




final private[immutable] case class LongTrieSetL3(
                                                     v1: Long,
                                                     v2: Long,
                                                     v3 : Long
                                                 ) extends LongTrieSetL {

    override def size : Int= 3

    override def contains( v : Long ): Boolean  = v == v1 ||v == v2 || v == v3

    override def foreach[U](f: Long ⇒ U): Unit = {f(v1); f(v2); f(v3)}


    override def toString(level: Int): String = {
        s"L3(${v1.toBinaryString}=$v1, ${v2.toBinaryString}=$v2, ${v3.toBinaryString}=$v3)"
    }


    override  def split_0(level: Int): LongTrieSetNode = {
        if ((((v1 >> level) & 1L) == 0L)) {
            if ((((v2 >> level) & 1L) == 0L)) {
                if ((((v3 >> level) & 1L) == 0L)) {
                  this
                } else {
                    new LongTrieSetL2(v1,v2)
                }
            } else {
                if ((((v3 >> level) & 1L) == 0L)) {
                    new LongTrieSetL2(v1,v3)
                } else {
                    new LongTrieSetL1(v1)
                }
            }
        } else {
            if ((((v2 >> level) & 1L) == 0L)) {
                if ((((v3 >> level) & 1L) == 0L)) {
                    new LongTrieSetL2(v2,v3)
                } else {
                    new LongTrieSetL1(v2)
                }
            } else {
                if ((((v3 >> level) & 1L) == 0L)) {
                    new LongTrieSetL1(v3)
                } else {
                    null
                }
            }
        }
    }

    override     def split_1(level: Int): LongTrieSetNode = {
        if ((((v1 >> level) & 1L) == 0L)) {
            if ((((v2 >> level) & 1L) == 0L)) {
                if ((((v3 >> level) & 1L) == 0L)) {
                    null
                } else {
                    new LongTrieSetL1(v3)
                }
            } else {
                if ((((v3 >> level) & 1L) == 0L)) {
                    new LongTrieSetL1(v2)
                } else {
                    new LongTrieSetL2(v2,v3)
                }
            }
        } else {
            if ((((v2 >> level) & 1L) == 0L)) {
                if ((((v3 >> level) & 1L) == 0L)) {
                    new LongTrieSetL1(v1)
                } else {
                    new LongTrieSetL2(v1,v3)
                }
            } else {
                if ((((v3 >> level) & 1L) == 0L)) {
                    new LongTrieSetL2(v1,v2)
                } else {
                    this
                }
            }
        }
    }

    override def +(v: Long , level: Int): LongTrieSetNode = {
        val thisV1 = this.v1
        val thisV2 = this.v2
        val thisV3 = this.v3
        if (v == thisV1 ||v == thisV2 ||v == thisV3)
            return this;

        val l = new LongTrieSetL1(v)
        // Let's check if there is some sharing and if so, let's use it.
        val thisV1Shifted = thisV1 >> level
        val vShifted = v >> level
        val newNode =         JLong.numberOfTrailingZeros(thisV1Shifted ^ vShifted) match {
            case 0 ⇒
                if ((thisV1Shifted & 1L) == 0L) {
                    new LongTrieSetN2(this, l)
                } else {
                    new LongTrieSetN2(l, this)
                }
            case 1 ⇒
                if ((thisV1Shifted & 1L) == 0L) {
                    if (((thisV1Shifted >> 1) & 1L) == 0L) {
                        new LongTrieSetN4(this, null, l, null)
                    } else {
                        new LongTrieSetN4(l, null, this, null)
                    }
                } else {
                    if (((thisV1Shifted >> 1) & 1L) == 0L) {
                        new LongTrieSetN4(null, this, null, l)
                    } else {
                        new LongTrieSetN4(null, l, null, this)
                    }
                }
            case length ⇒
                val sharedBits = thisV1Shifted & LongSet.BitMasks(length)
                val n =
                    if (((thisV1Shifted >> length) & 1L) == 0L)
                        new LongTrieSetN2(this, l)
                    else
                        new LongTrieSetN2(l, this)
                LongTrieSetNShared(sharedBits, length, n)
        }
      newNode +(v2,level) + (v3,level)
    }


}

/** The inner nodes of the trie set. */
private[immutable] abstract class LongTrieSetInnerNode extends LongTrieSetNode {
    final override def isN: Boolean = true
    final override def isL: Boolean = false
    final override def asL: LongTrieSetL = throw new ClassCastException()
}

private[immutable] abstract class LongTrieSetNShared extends LongTrieSetInnerNode {

    final override def isN4: Boolean = false

    def sharedBits: Long
    def length: Int // at least "1"
    def n: LongTrieSetNode

    final override def foreach[U](f: Long ⇒ U): Unit =  n.foreach(f)

    final override def +(v : Long, level: Int): LongTrieSetNode = {
        val length = this.length
        val sharedBits = this.sharedBits
        val lValue = v
        val lValueShifted = lValue >> level
        if ((lValueShifted & LongSet.BitMasks(length)) == sharedBits) {
            val oldN = this.n
            val newN = oldN + (v, level + length)
            if (oldN ne newN) {
                LongTrieSetNShared(sharedBits, length, newN)
            } else {
                this
            }
        } else {
            // `length` is at least 1 and the number of shared bits is smaller than the current
            // length; i.e., lengthOfTail is at least 1
            val lengthOfLead = JLong.numberOfTrailingZeros(sharedBits ^ lValueShifted)
            val lengthOfTail = length - lengthOfLead - 1 /* -1 for the differing bit */

            // Potential optimizations:
            // We can fold the tail if the number of shared remaining bits is one
            // We can fold the lead if the number of shared initial bits is one

            // 1. Create new tail (if required):
            val newT =
                if (lengthOfTail == 0)
                    n
                else
                    LongTrieSetNShared(sharedBits >> lengthOfLead + 1, lengthOfTail, n)
            // 2. Create new node where we have the difference
            val newM =
                if (((sharedBits >> lengthOfLead) & 1L) == 0L) {
                    LongTrieSetN2(level + lengthOfLead, newT, new LongTrieSetL1(v))
                } else {
                    LongTrieSetN2(level + lengthOfLead, new LongTrieSetL1(v), newT)
                }
            // 3. Create new lead node (if required)
            if (lengthOfLead == 0) {
                newM
            } else {
                LongTrieSetNShared(sharedBits & LongSet.BitMasks(lengthOfLead), lengthOfLead, newM)
            }

        }
    }

    def toString(level: Int): String = {
        val lP2 = level + length + 2
        s"NShared(_${sharedBits.toBinaryString}(#$length)=>${n.toString(lP2)}"
    }
}

private[immutable] object LongTrieSetNShared {

    def apply(sharedBits: Long, length: Int, n: LongTrieSetNode): LongTrieSetNode = {

        assert(length >= 1)

        (length: @switch) match {
            case 1 ⇒
                // if (sharedBits == 0L /*test the last bit...*/ )
                //    new LongTrieSetNShared_0(n)
                // else
                //    new LongTrieSetNShared_1(n)
                (n: @unchecked) match {
                    case l: LongTrieSetN2 ⇒
                        if (sharedBits == 0L /*test the last bit...*/ ) {
                            new LongTrieSetN4(l._0, null, l._1, null)
                        } else {
                            new LongTrieSetN4(null, l._0, null, l._1)
                        }
                    case l: LongTrieSetN4 ⇒
                        if (sharedBits == 0L /*test the last bit...*/ )
                            new LongTrieSetNShared_0(n)
                        else
                            new LongTrieSetNShared_1(n)
                    // [will never occur!] case l: LongTrieSetL       ⇒ ...
                    // [will never occur!] case l: LongTrieSetNShared ⇒ ...
                }

            case 2 ⇒
                (sharedBits.toInt: @switch) match {
                    case 0 ⇒ new LongTrieSetNShared_00(n)
                    case 1 ⇒ new LongTrieSetNShared_01(n)
                    case 2 ⇒ new LongTrieSetNShared_10(n)
                    case 3 ⇒ new LongTrieSetNShared_11(n)
                }

            case _ ⇒ new LongTrieSetNShared_X(sharedBits, length, n)
        }

    }

}

final private[immutable] class LongTrieSetNShared_0(
        val n: LongTrieSetNode
) extends LongTrieSetNShared {
    override def sharedBits: Long = 0
    override def length: Int = 1
    override def split_0(level: Int): LongTrieSetNode = n
    override def split_1(level: Int): LongTrieSetNode = null

}

final private[immutable] class LongTrieSetNShared_1(
        val n: LongTrieSetNode
) extends LongTrieSetNShared {
    override def sharedBits: Long = 1
    override def length: Int = 1
    override def split_0(level: Int): LongTrieSetNode = null
    override def split_1(level: Int): LongTrieSetNode = n

}

final private[immutable] class LongTrieSetNShared_00(
        val n: LongTrieSetNode
) extends LongTrieSetNShared {
    override def sharedBits: Long = 0
    override def length: Int = 2
    override def split_0(level: Int): LongTrieSetNode = new LongTrieSetNShared_0(n)
    override def split_1(level: Int): LongTrieSetNode = null
}

final private[immutable] class LongTrieSetNShared_01(
        val n: LongTrieSetNode
) extends LongTrieSetNShared {
    def sharedBits: Long = 1
    def length: Int = 2
    override def split_0(level: Int): LongTrieSetNode = null
    override def split_1(level: Int): LongTrieSetNode = new LongTrieSetNShared_0(n)
}

final private[immutable] class LongTrieSetNShared_10(
        val n: LongTrieSetNode
) extends LongTrieSetNShared {
    def sharedBits: Long = 2
    def length: Int = 2
    override def split_0(level: Int): LongTrieSetNode = new LongTrieSetNShared_1(n)
    override def split_1(level: Int): LongTrieSetNode = null
}

final private[immutable] class LongTrieSetNShared_11(
        val n: LongTrieSetNode
) extends LongTrieSetNShared {
    def sharedBits: Long = 3
    def length: Int = 2
    override def split_0(level: Int): LongTrieSetNode = null
    override def split_1(level: Int): LongTrieSetNode = new LongTrieSetNShared_1(n)
}

final private[immutable] class LongTrieSetNShared_X(
        val sharedBits: Long,
        val length:     Int,
        val n:          LongTrieSetNode
) extends LongTrieSetNShared {
    override def split_0(level: Int): LongTrieSetNode = {
        if ((sharedBits & 1L) == 0L) {
            LongTrieSetNShared(sharedBits >> 1, length - 1, n)
        } else {
            null
        }
    }
    override def split_1(level: Int): LongTrieSetNode = {
        if ((sharedBits & 1L) == 1L) {
            LongTrieSetNShared(sharedBits >> 1, length - 1, n)
        } else {
            null
        }
    }
}

/** The inner nodes of the trie set. */
final private[immutable] class LongTrieSetN2(
        val _0: LongTrieSetNode,
        val _1: LongTrieSetNode
) extends LongTrieSetInnerNode {

    override def isN4: Boolean = false

    override def foreach[U](f: Long ⇒ U): Unit =  {_0.foreach(f);_1.foreach(f)}

    def +(v : Long, level: Int): LongTrieSetNode = {
        val _0 = this._0
        val _1 = this._1
        val lLSB = (v >> level) & 1L // lsb == bit at index `level`

        if (_0.isN || _1.isN) {
            // We can't get rid of this N2 node... a successor node is an inner node and we
            // do not want to perform "large" changes to the overall trie.
            return {
                if (lLSB == 0) {
                    val new_0 = _0 + (v, level + 1)
                    if (_0 ne new_0) {
                        new LongTrieSetN2(new_0, this._1)
                    } else {
                        this
                    }
                } else {
                    val new_1 = _1 + (v, level + 1)
                    if (_1 ne new_1) {
                        new LongTrieSetN2(this._0, new_1)
                    } else {
                        this
                    }
                }
            };
        }

        if (lLSB == 0) {
            val _0L = _0.asL
            if(!_0L.contains(v)) {
              if(_0L.size == 3) {
                 val n = new LongTrieSetN4(null,null,null,null)
                    n += (v,level)
                 _0L.foreach(v ⇒ n += (v,level))
                 _1.asL.foreach(v ⇒ n += (v,level))
                n
              } else {
                  new LongTrieSetN2(_0+(v,level),_1)
              }
            } else {
              this
            }
        } else {
            val _1L = _1.asL
            if(!_1L.contains(v)) {
                if(_1L.size == 3) {
                    val n = new LongTrieSetN4(null,null,null,null)
                    n += (v,level)
                    _1L.foreach(v ⇒ n += (v,level))
                    _0.asL.foreach(v ⇒ n += (v,level))
                    n
                } else {
                  new LongTrieSetN2(_0,_1L + (v,level))
                }
            } else {
                this
            }
        }
    }

    override def split_0(level: Int): LongTrieSetNode = _0

    override def split_1(level: Int): LongTrieSetNode = _1

    override def toString(level: Int): String = {
        val indent = " " * level
        val lP2 = level + 2
        s"N2(\n$indent 0=>${_0.toString(lP2)}\n$indent 1=>${_1.toString(lP2)})"
    }
}

private[immutable] object LongTrieSetN2 {

    def apply(
        level: Int,
        _0:    LongTrieSetNode,
        _1:    LongTrieSetNode
    ): LongTrieSetNode = {
        if (_0.isN4 || _1.isN4)
            return new LongTrieSetN2(_0, _1);

        val newLevel = level + 1
        new LongTrieSetN4(
            _0.split_0(newLevel), _1.split_0(newLevel), _0.split_1(newLevel), _1.split_1(newLevel)
        )
    }

}

/** The inner nodes of the trie set. */
final private[immutable] class LongTrieSetN4(
        // least significant bits _ (current) second most important bit _ (current) most important bit
        private[immutable] var _0: LongTrieSetNode, // a tree node, a leaf node or null
        private[immutable] var _1: LongTrieSetNode, // a tree node, a leaf node or null
        private[immutable] var _2: LongTrieSetNode, // a tree node, a leaf node or null
        private[immutable] var _3: LongTrieSetNode // a tree node, a leaf node or null
) extends LongTrieSetInnerNode {

    override def isN4: Boolean = true

    override def foreach[U](f: Long ⇒ U): Unit =  {
        val _0 = this._0
      if(_0 ne null) _0.foreach(f)
        val _1 = this._1
        if(_1 ne null) _1.foreach(f)
        val _2 = this._2
        if(_2 ne null) _2.foreach(f)
        val _3 = this._3
        if(_3 ne null) _3.foreach(f)
    }

    override def +(v: Long, level: Int): LongTrieSetNode = {
        // Basic assumption: the trie is nearly balanced...
        val consideredBits = ((v >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 ⇒
                val _0 = this._0
                if (_0 == null) {
                    new LongTrieSetN4(new LongTrieSetL1(v), this._1, this._2, this._3)
                } else {
                    val new0 = _0 + (v, level + 2)
                    if (_0 ne new0) {
                        new LongTrieSetN4(new0, this._1, this._2, this._3)
                    } else {
                        this
                    }
                }
            case 1 ⇒
                val _1 = this._1
                if (_1 == null) {
                    new LongTrieSetN4(this._0, new LongTrieSetL1(v), this._2, this._3)
                } else {
                    val new1 = _1 + (v, level + 2)
                    if (_1 ne new1) {
                        new LongTrieSetN4(this._0, new1, this._2, this._3)
                    } else {
                        this
                    }
                }

            case 2 ⇒
                val _2 = this._2
                if (_2 == null) {
                    new LongTrieSetN4(this._0, this._1, new LongTrieSetL1(v), this._3)
                } else {
                    val new2 = _2 + (v, level + 2)
                    if (_2 ne new2) {
                        new LongTrieSetN4(this._0, this._1, new2, this._3)
                    } else {
                        this
                    }
                }

            case 3 ⇒
                val _3 = this._3
                if (_3 == null) {
                    new LongTrieSetN4(this._0, this._1, this._2, new LongTrieSetL1(v))
                } else {
                    val new3 = _3 + (v, level + 2)
                    if (_3 ne new3) {
                        new LongTrieSetN4(this._0, this._1, this._2, new3)
                    } else {
                        this
                    }
                }
        }
    }

     def +=(v: Long, level: Int): Unit = {
        val consideredBits = ((v >> level) & 3L).toInt
        (consideredBits: @switch) match {
            case 0 ⇒
                if (_0 == null) {
                    _0 = new LongTrieSetN4(new LongTrieSetL1(v), this._1, this._2, this._3)
                } else {
                    _0 = _0 + (v, level + 2)
                }
            case 1 ⇒
                if (_1 == null) {
                  _1 = new LongTrieSetN4(this._0, new LongTrieSetL1(v), this._2, this._3)
                } else {
                    _1 = _1 + (v, level + 2)
                }

            case 2 ⇒
                if (_2 == null) {
                    _2 = new LongTrieSetN4(this._0, this._1, new LongTrieSetL1(v), this._3)
                } else {
                  _2 = _2 + (v, level + 2)
                }

            case 3 ⇒
                if (_3 == null) {
                   _3 =  new LongTrieSetN4(this._0, this._1, this._2, new LongTrieSetL1(v))
                } else {
                    _3 = _3 + (v, level + 2)
                }
        }
    }

    override def split_0(level: Int): LongTrieSetNode = throw new UnknownError()
    override def split_1(level: Int): LongTrieSetNode = throw new UnknownError()

    override def toString(level: Int): String = {
        val indent = " " * level
        val lP2 = level + 4
        "N4("+
            s"\n$indent 00=>${if (_0 ne null) _0.toString(lP2) else null}"+
            s"\n$indent 01=>${if (_1 ne null) _1.toString(lP2) else null}"+
            s"\n$indent 10=>${if (_2 ne null) _2.toString(lP2) else null}"+
            s"\n$indent 11=>${if (_3 ne null) _3.toString(lP2) else null})"
    }
}

object LargeLongTrieSet {
    final val InitialBucketsCount = 16 // MUST BE a power of 2; i.e., 2^x!

    def initialBucketsCount: Int = InitialBucketsCount
}

private[immutable] class LargeLongTrieSet(
        var size:  Int                          = 0,
        val tries: Array[LongTrieSetNode] = new Array(LargeLongTrieSet.InitialBucketsCount),
) extends LongTrieSet { set ⇒

    final override def isEmpty: Boolean = size == 0
    final override def nonEmpty: Boolean = true
    final override def isSingletonSet: Boolean = size == 1

    final override def contains(v: Long): Boolean = {
        val tries = this.tries
        val trie = tries(Math.abs(JLong.hashCode(v)) % tries.length)
        if (trie == null) return false;

        var key = v
        var node = trie
        do {
            // Type based switch (proofed to be faster than introducing node ids and using them...):
            node match {
                case n: LongTrieSetNShared ⇒
                    val sharedBits = n.sharedBits
                    val length = n.length
                    if ((key & LongSet.BitMasks(length)) == sharedBits) {
                        node = n.n
                        key = key >> n.length
                    } else {
                        return false;
                    }
                case n: LongTrieSetN2 ⇒
                    if ((key & 1L) == 0L) {
                        node = n._0
                    } else {
                        node = n._1
                    }
                    key = key >> 1
                case n: LongTrieSetN4 ⇒
                    ((key & 3L /*binary:11*/ ).toInt: @switch) match {
                        case 0 ⇒ node = n._0
                        case 1 ⇒ node = n._1
                        case 2 ⇒ node = n._2
                        case 3 ⇒ node = n._3
                    }
                    key = key >> 2
                case l: LongTrieSetL ⇒
                    return l.contains(v)
            }
        } while (node ne null)
        false
    }

    override def foreach[U](f: Long ⇒ U): Unit =  {
      val tries = this.tries
      var i = tries.length
      do {
        i -= 1
        val trie = tries(i)
            if(trie ne null) trie.foreach(f)
      } while(i> 0)
    }

    final override def +(v: Long): LargeLongTrieSet = {
        def extend(s: LargeLongTrieSet, v: Long): Unit = {
            val tries = s.tries
            val trieId = abs(lHashCode(v)) % tries.length
            val oldTrie = tries(trieId)
            if (oldTrie == null) {
                tries(trieId) = new LongTrieSetL1(v)
            } else {
                tries(trieId) = oldTrie + (v, 0)
            }
        }

        def rehash(newSize: Int, bucketsCount: Int): LargeLongTrieSet = {
            val newLLLTS = new LargeLongTrieSet(newSize, new Array(bucketsCount))
            extend(newLLLTS,v)
            foreach{v ⇒ extend(newLLLTS,v)}
          newLLLTS
        }

        val tries = this.tries
        val trieId = abs(lHashCode(v)) % tries.length
        val oldTrie = tries(trieId)
        if (oldTrie == null) {
            val newSize = size + 1
            newSize match {
                case 24 ⇒ rehash(newSize, 32)
                case 48 ⇒ rehash(newSize, 64)
                case _ ⇒
                    val newTries = tries.clone()
                    newTries(trieId) = new LongTrieSetL1(v)
                    new LargeLongTrieSet(newSize, newTries)
            }
        } else {
            val newTrie = oldTrie + (v, 0)
            if (oldTrie ne newTrie) {
                val newSize = size + 1
                newSize match {
                    case 24 ⇒ rehash(newSize, 32)
                    case 48 ⇒ rehash(newSize, 64)
                    case _ ⇒
                        val newTries = tries.clone()
                        newTries(trieId) = newTrie
                        new LargeLongTrieSet(newSize, newTries)
                }
            } else {
                this
            }
        }
    }

    final private[immutable] def +=(v: Long): Unit = {
        val tries = this.tries
        val trieId = abs(lHashCode(v)) % tries.length
        val oldTrie = tries(trieId)
        if (oldTrie == null) {
            tries(trieId) = new LongTrieSetL1(v)
            size += 1
        } else {
            val newTrie = oldTrie + (v, 0)
            if (oldTrie ne newTrie) {
                tries(trieId) = newTrie
                size += 1
            }
        }
    }

  /*
    final override def equals(other: Any): Boolean = {
        other match {
            case that: LargeLongTrieSet ⇒ (this eq that) || {
                this.size == that.size &&
                    this.iterator.sameValues(that.iterator)
            }
            case _ ⇒ false
        }
    }

    final override def hashCode(): Int = {
      // IMPROVE Implement naive foldLeft
        iterator.foldLeft(0)((hashCode, v) ⇒ (hashCode * 31) + java.lang.Long.hashCode(v))
    }
*/
    final override def toString: String = {
        val triesString =
            tries.
                zipWithIndex.
                map { e ⇒
                    val (trie, index) = e
                    s"[$index] "+(if (trie ne null) trie.toString(0) else "N/A")
                }.
                mkString("\n")
        s"LongTrieSet(#$size,tries=\n$triesString\n)"
    }

}

