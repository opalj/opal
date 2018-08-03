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
sealed trait CalleesPropertyMetaInformation extends PropertyMetaInformation {

    final type Self = Callees
}

sealed trait Callees extends Property with OrderedProperty with CalleesPropertyMetaInformation {

    def size: Int

    def callees(pc: Int)(implicit declaredMethods: DeclaredMethods): Set[DeclaredMethod]

    def callees(implicit declaredMethods: DeclaredMethods): Iterator[(Int, Set[DeclaredMethod])]

    override def toString: String = {
        s"Callees(size=${this.size})"
    }

    final def key: PropertyKey[Callees] = Callees.key

    override def checkIsEqualOrBetterThan(e: Entity, other: Callees): Unit = {
        if (other.size < size)
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
    }
}

final class CalleesImplementation(
        private[this] val calleesIds: IntMap[IntTrieSet]
) extends Callees {

    override def callees(pc: Int)(implicit declaredMethods: DeclaredMethods): Set[DeclaredMethod] = {
        calleesIds(pc).mapToAny[DeclaredMethod](declaredMethods.apply)
    }

    override def callees(
        implicit
        declaredMethods: DeclaredMethods
    ): Iterator[(Int, Set[DeclaredMethod])] = {
        calleesIds.iterator.map {
            case (pc, x) ⇒ {
                pc → x.mapToAny[DeclaredMethod](declaredMethods.apply)
            }
        }
    }

    override val size: Int = {
        calleesIds.iterator.map(_._2.size).sum
    }
}

object LowerBoundCallees extends Callees {

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

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        PropertyKey.create(
            "Callees",
            (_: PropertyStore, r: FallbackReason, _: DeclaredMethod) ⇒ {
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        LowerBoundCallees
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        //println(s"Fallback callee $m")
                        new CalleesImplementation(IntMap.empty)
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, Callees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }
}
