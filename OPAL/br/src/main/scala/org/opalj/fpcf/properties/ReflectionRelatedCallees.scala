/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * Callees that exist because of the use of reflection.
 *
 * @author Dominik Helm
 */
sealed trait ReflectionRelatedCalleesPropertyMetaInformation
    extends IndirectCalleesPropertyMetaInformation {

    final type Self = ReflectionRelatedCallees
}

sealed trait ReflectionRelatedCallees extends IndirectCallees
    with ReflectionRelatedCalleesPropertyMetaInformation {

    override def toString: String = {
        s"ReflectionRelatedCallees(size=${this.size})"
    }

    final def key: PropertyKey[ReflectionRelatedCallees] = ReflectionRelatedCallees.key
}

sealed class ReflectionRelatedCalleesImplementation(
        private[properties] val calleesIds: IntMap[IntTrieSet]
) extends ReflectionRelatedCallees with IndirectCalleesImplementation

object LowerBoundReflectionRelatedCallees extends ReflectionRelatedCallees
    with IndirectCalleesLowerBound

object NoReflectionRelatedCallees extends ReflectionRelatedCalleesImplementation(IntMap.empty)

object NoReflectionRelatedCalleesDueToNotReachableMethod extends ReflectionRelatedCallees
    with IndirectCalleesNotReachable

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
