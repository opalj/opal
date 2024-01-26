/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package collection
package immutable

object Pair {

    def apply(v1: Int, v2: Int): IntIntPair = IntIntPair(v1, v2)

    def apply[T <: AnyRef](v1: Long, v2: T) = new LongRefPair[T](v1, v2)

    // The code optimizer should be able to remove the (un)boxing logic.
    def unapply(p: IntIntPair): Some[(Int, Int)] = Some((p.first, p.second))

    def unapply[T <: AnyRef](p: LongRefPair[T]): Some[(Long, T)] = Some((p.first, p.second))

}
