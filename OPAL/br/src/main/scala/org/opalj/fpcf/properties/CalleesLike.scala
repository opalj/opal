/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * For a given [[DeclaredMethod]], and for each call site (represented by the PC), the set of methods
 * that are possible call targets.
 *
 * @author Florian Kuebler
 */
trait CalleesLike extends OrderedProperty with CalleesLikePropertyMetaInformation {

    def size: Int

    def callees(pc: Int)(implicit declaredMethods: DeclaredMethods): Set[DeclaredMethod]

    def callees(implicit declaredMethods: DeclaredMethods): Iterator[(Int, Set[DeclaredMethod])]

    def updated(pc: Int, callee: DeclaredMethod)(implicit declaredMethods: DeclaredMethods): Self

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other.size < size)
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

trait CalleesLikeImplementation extends CalleesLike {
    private[properties] val calleesIds: IntMap[IntTrieSet]

    override def callees(pc: Int)(implicit declaredMethods: DeclaredMethods): Set[DeclaredMethod] = {
        calleesIds(pc).mapToAny[DeclaredMethod](declaredMethods.apply)
    }

    override def callees(
        implicit
        declaredMethods: DeclaredMethods
    ): Iterator[(Int, Set[DeclaredMethod])] = {
        calleesIds.iterator.map {
            case (pc, x) ⇒
                pc → x.mapToAny[DeclaredMethod](declaredMethods.apply)
        }
    }

    override val size: Int = {
        calleesIds.iterator.map(_._2.size).sum
    }
}

trait CalleesLikeLowerBound extends CalleesLike {
    override lazy val size: Int = {
        Int.MaxValue
    }

    override def callees(
        pc: Int
    )(implicit declaredMethods: DeclaredMethods): Set[DeclaredMethod] = {
        throw new UnsupportedOperationException()
    }

    override def callees(
        implicit
        declaredMethods: DeclaredMethods
    ): Iterator[(UShort, Set[DeclaredMethod])] = throw new UnsupportedOperationException()
}

trait CalleesLikePropertyMetaInformation extends PropertyMetaInformation {

    override type Self <: CalleesLike
}