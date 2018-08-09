/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * TODO
 *
 * @author Florian Kuebler
 */
sealed trait ThreadRelatedCalleesPropertyMetaInformation
        extends CalleesLikePropertyMetaInformation {

    final type Self = ThreadRelatedCallees
}

sealed trait ThreadRelatedCallees extends CalleesLike
        with ThreadRelatedCalleesPropertyMetaInformation {

    override def toString: String = {
        s"ThreadRelatedCallees(size=${this.size})"
    }

    final def key: PropertyKey[ThreadRelatedCallees] = ThreadRelatedCallees.key
}

sealed class ThreadRelatedCalleesImplementation(
        private[properties] val calleesIds: IntMap[IntTrieSet]
) extends ThreadRelatedCallees with CalleesLikeImplementation {
    override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): ThreadRelatedCallees = {
        if (calleesIds.contains(pc) && calleesIds(pc).contains(callee.id)) {
            this
        } else {
            val old = calleesIds.getOrElse(pc, IntTrieSet.empty)
            val newCalleesIds = calleesIds.updated(pc, old + callee.id)
            new ThreadRelatedCalleesImplementation(newCalleesIds)
        }
    }
}

object LowerBoundThreadRelatedCallees extends ThreadRelatedCallees with CalleesLikeLowerBound {
    override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): ThreadRelatedCallees = this
}

object NoThreadRelatedCallees extends ThreadRelatedCalleesImplementation(IntMap.empty)

object NoThreadRelatedCalleesDueToNotReachableMethod extends ThreadRelatedCallees
    with CalleesLikeNotReachable

object ThreadRelatedCallees extends ThreadRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[ThreadRelatedCallees] = {
        PropertyKey.create(
            "ThreadRelatedCallees",
            (_: PropertyStore, r: FallbackReason, _: DeclaredMethod) ⇒ {
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        LowerBoundThreadRelatedCallees
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        NoThreadRelatedCalleesDueToNotReachableMethod
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, ThreadRelatedCallees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }
}
