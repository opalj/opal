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

sealed trait Callees extends CalleesLike with CalleesPropertyMetaInformation {

    override def toString: String = {
        s"Callees(size=${this.size})"
    }

    final def key: PropertyKey[Callees] = Callees.key
}

sealed class CalleesImplementation(
        private[properties] val calleesIds: IntMap[IntTrieSet]
) extends Callees with CalleesLikeImplementation {
    override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): Callees = {
        if (calleesIds.contains(pc) && calleesIds(pc).contains(callee.id)) {
            this
        } else {
            val old = calleesIds.getOrElse(pc, IntTrieSet.empty)
            val newCalleesIds = calleesIds.updated(pc, old + callee.id)
            new CalleesImplementation(newCalleesIds)
        }
    }
}

object LowerBoundCallees extends Callees with CalleesLikeLowerBound {
    override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): Callees = this
}

object NoCallees extends CalleesImplementation(IntMap.empty)

object Callees extends CalleesPropertyMetaInformation {

    final val key: PropertyKey[Callees] = {
        PropertyKey.create(
            "Callees",
            (_: PropertyStore, r: FallbackReason, _: DeclaredMethod) ⇒ {
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        LowerBoundCallees
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        NoCallees
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, Callees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }
}
