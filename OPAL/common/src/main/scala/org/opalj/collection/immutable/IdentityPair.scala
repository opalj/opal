/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

/**
 * Encapsulates a pair of values that is intended to be used as a key in Maps.
 * Compared to a standard pair (Tuple2), however, comparison of two `IdentityPair`
 * objects is done by doing a reference-based comparison of the stored values.
 *
 * @example
 * {{{
 *  val a = new String("fooBar")
 *  val b = "foo"+"Bar"
 *  val p1 = new IdentityPair(a,b) // #1
 *  val p2 = new IdentityPair(a,a) // #2
 *  val p3 = new IdentityPair(a,b) // #3
 *  p1 == p2 // => false (though (a,b) == (a,a) would be true
 *  p1 == p3 // => true
 *  }}}
 *
 * @param _1 A reference value (can be `null`).
 * @param _2 A reference value (can be `null`).
 *
 * @author Michael Eichberg
 */
final case class IdentityPair[+T1 <: AnyRef, +T2 <: AnyRef](
        _1: T1,
        _2: T2
) extends Product2[T1, T2] {

    override def canEqual(other: Any): Boolean = other.isInstanceOf[IdentityPair[_, _]]

    override def equals(other: Any): Boolean = {
        // We don't need to call canEqual because this class is final!
        other match {
            case that: IdentityPair[_, _] => (this._1 eq that._1) && (this._2 eq that._2)
            case _                        => false
        }
    }

    /*
    @volatile private[this] var hash: Int = 0
    override def hashCode: Int = {
        var hash = this.hash
        if (hash == 0) {
            hash = identityHashCode(_1) * 113 + identityHashCode(_2)
            this.hash = hash
        }
        hash
    }
    */
    override def hashCode: Int = System.identityHashCode(_1) * 31 + System.identityHashCode(_2)
}
