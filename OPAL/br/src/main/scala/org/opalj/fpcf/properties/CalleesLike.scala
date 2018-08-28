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
 * @author Dominik Helm
 */
trait CalleesLike extends OrderedProperty with CalleesLikePropertyMetaInformation {

    def size: Int

    def callees(pc: Int)(implicit declaredMethods: DeclaredMethods): Option[Set[DeclaredMethod]]

    def callsites(implicit declaredMethods: DeclaredMethods): Map[Int, Set[DeclaredMethod]]

    private[fpcf] /*todo better package*/ def encodedCallees: IntMap[IntTrieSet]

    //def updated(pc: Int, callee: DeclaredMethod)(implicit declaredMethods: DeclaredMethods): Self

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other.size < size)
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

trait CalleesLikeImplementation extends CalleesLike {
    private[properties] val calleesIds: IntMap[IntTrieSet]

    override def callees(
        pc: Int
    )(implicit declaredMethods: DeclaredMethods): Option[Set[DeclaredMethod]] = {
        calleesIds.get(pc).map(_.mapToAny[DeclaredMethod](declaredMethods.apply))
    }

    override def callsites(
        implicit
        declaredMethods: DeclaredMethods
    ): Map[Int, Set[DeclaredMethod]] = {
        calleesIds.mapValues(_.mapToAny[DeclaredMethod](declaredMethods.apply))
    }

    override val size: Int = {
        calleesIds.iterator.map(_._2.size).sum
    }

    override private[fpcf] def encodedCallees: IntMap[IntTrieSet] = calleesIds
}

trait CalleesLikeLowerBound extends CalleesLike {
    override def size: Int = {
        Int.MaxValue
    }

    override def callees(
        pc: Int
    )(implicit declaredMethods: DeclaredMethods): Option[Set[DeclaredMethod]] = {
        throw new UnsupportedOperationException()
    }

    override def callsites(
        implicit
        declaredMethods: DeclaredMethods
    ): Map[Int, Set[DeclaredMethod]] = throw new UnsupportedOperationException()

    override private[fpcf] def encodedCallees: IntMap[IntTrieSet] = IntMap.empty
}

trait CalleesLikeNotReachable extends CalleesLike {
    override def size: Int = 0

    override def callsites(
        implicit
        declaredMethods: DeclaredMethods
    ): Map[Int, Set[DeclaredMethod]] = Map.empty

    override def callees(
        pc: Int
    )(implicit declaredMethods: DeclaredMethods): Option[Set[DeclaredMethod]] = None

    override private[fpcf] def encodedCallees: IntMap[IntTrieSet] = IntMap.empty

    /* override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): Self = throw new UnsupportedOperationException()*/
}

trait CalleesLikePropertyMetaInformation extends PropertyMetaInformation {

    override type Self <: CalleesLike

    val isIndirect: Boolean
}

trait DirectCallees extends CalleesLike
trait DirectCalleesImplementation extends CalleesLikeImplementation
trait DirectCalleesLowerBound extends CalleesLikeLowerBound
trait DirectCalleesNotReachable extends CalleesLikeNotReachable
trait DirectCalleesPropertyMetaInformation extends CalleesLikePropertyMetaInformation {
    override type Self <: DirectCallees
    final override val isIndirect = false
}

trait IndirectCallees extends CalleesLike
trait IndirectCalleesImplementation extends CalleesLikeImplementation
trait IndirectCalleesLowerBound extends CalleesLikeLowerBound
trait IndirectCalleesNotReachable extends CalleesLikeNotReachable
trait IndirectCalleesPropertyMetaInformation extends CalleesLikePropertyMetaInformation {
    override type Self <: IndirectCallees
    final override val isIndirect = true
}