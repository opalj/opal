/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * Callees that exist because of the use of reflection.
 *
 * @author Dominik Helm
 */
sealed trait ReflectionRelatedCalleesPropertyMetaInformation
    extends CalleesLikePropertyMetaInformation {

    final type Self = ReflectionRelatedCallees
}

sealed trait ReflectionRelatedCallees extends CalleesLike
    with ReflectionRelatedCalleesPropertyMetaInformation {

    override def toString: String = {
        s"ReflectionRelatedCallees(size=${this.size})"
    }

    final def key: PropertyKey[ReflectionRelatedCallees] = ReflectionRelatedCallees.key
}

sealed class ReflectionRelatedCalleesImplementation(
        private[properties] val calleesIds: IntMap[IntTrieSet]
) extends ReflectionRelatedCallees with CalleesLikeImplementation {
    override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): ReflectionRelatedCallees = {
        if (calleesIds.contains(pc) && calleesIds(pc).contains(callee.id)) {
            this
        } else {
            val old = calleesIds.getOrElse(pc, IntTrieSet.empty)
            val newCalleesIds = calleesIds.updated(pc, old + callee.id)
            new ReflectionRelatedCalleesImplementation(newCalleesIds)
        }
    }
}

object LowerBoundReflectionRelatedCallees extends ReflectionRelatedCallees
    with CalleesLikeLowerBound {
    override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): ReflectionRelatedCallees = this
}

object NoReflectionRelatedCallees extends ReflectionRelatedCalleesImplementation(IntMap.empty)

object NoReflectionRelatedCalleesDueToNotReachableMethod extends ReflectionRelatedCallees
    with CalleesLikeNotReachable

object ReflectionRelatedCallees extends ReflectionRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[ReflectionRelatedCallees] = {
        PropertyKey.create(
            "ReflectionRelatedCallees",
            (_: PropertyStore, r: FallbackReason, _: DeclaredMethod) ⇒ {
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        LowerBoundReflectionRelatedCallees
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        NoReflectionRelatedCalleesDueToNotReachableMethod
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, ReflectionRelatedCallees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }
}
