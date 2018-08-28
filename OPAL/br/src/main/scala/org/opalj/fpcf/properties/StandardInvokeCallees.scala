/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * For a given [[DeclaredMethod]], and for each call site (represented by the PC), the set of methods
 * that are possible call targets.
 *
 * @author Florian Kuebler
 */
sealed trait StandardInvokeCalleesPropertyMetaInformation
    extends DirectCalleesPropertyMetaInformation {

    final type Self = StandardInvokeCallees
}

sealed trait StandardInvokeCallees extends DirectCallees
    with StandardInvokeCalleesPropertyMetaInformation {

    override def toString: String = {
        s"StandardInvokeCallees(size=${this.size})"
    }

    final def key: PropertyKey[StandardInvokeCallees] = StandardInvokeCallees.key
}

sealed class StandardInvokeCalleesImplementation(
        private[properties] val calleesIds: IntMap[IntTrieSet]
) extends StandardInvokeCallees with DirectCalleesImplementation

object LowerBoundStandardInvokeCallees extends StandardInvokeCallees with DirectCalleesLowerBound

object NoStandardInvokeCallees extends StandardInvokeCalleesImplementation(IntMap.empty)

object NoStandardInvokeCalleesDueToNotReachableMethod extends StandardInvokeCallees
    with DirectCalleesNotReachable

object StandardInvokeCallees extends StandardInvokeCalleesPropertyMetaInformation {

    final val key: PropertyKey[StandardInvokeCallees] = {
        PropertyKey.create(
            "StandardInvokeCallees",
            (_: PropertyStore, r: FallbackReason, _: DeclaredMethod) ⇒ {
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        LowerBoundStandardInvokeCallees
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        NoStandardInvokeCalleesDueToNotReachableMethod
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, StandardInvokeCallees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }
}
