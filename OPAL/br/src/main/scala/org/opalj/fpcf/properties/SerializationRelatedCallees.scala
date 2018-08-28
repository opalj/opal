/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * TODO
 *
 * @author Florian Kuebler
 */
sealed trait SerializationRelatedCalleesPropertyMetaInformation
    extends IndirectCalleesPropertyMetaInformation {

    final type Self = SerializationRelatedCallees
}

sealed trait SerializationRelatedCallees extends IndirectCallees
    with SerializationRelatedCalleesPropertyMetaInformation {

    override def toString: String = {
        s"SerializationRelatedCallees(size=${this.size})"
    }

    final def key: PropertyKey[SerializationRelatedCallees] = SerializationRelatedCallees.key
}

sealed class SerializationRelatedCalleesImplementation(
        private[properties] val calleesIds: IntMap[IntTrieSet]
) extends SerializationRelatedCallees with IndirectCalleesImplementation

object LowerBoundSerializationRelatedCallees extends SerializationRelatedCallees
    with IndirectCalleesLowerBound

object NoSerializationRelatedCallees extends SerializationRelatedCalleesImplementation(IntMap.empty)

object NoSerializationRelatedCalleesDueToNotReachableMethod extends SerializationRelatedCallees
    with IndirectCalleesNotReachable

object SerializationRelatedCallees extends SerializationRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[SerializationRelatedCallees] = {
        PropertyKey.create(
            "SerializationRelatedCallees",
            (_: PropertyStore, r: FallbackReason, _: DeclaredMethod) ⇒ {
                r match {
                    case PropertyIsNotComputedByAnyAnalysis ⇒
                        LowerBoundSerializationRelatedCallees
                    case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                        NoSerializationRelatedCalleesDueToNotReachableMethod
                }
            },
            (_: PropertyStore, eps: EPS[DeclaredMethod, SerializationRelatedCallees]) ⇒ eps.ub,
            (_: PropertyStore, _: DeclaredMethod) ⇒ None
        )
    }
}
