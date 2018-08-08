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
sealed trait SerializationRelatedCalleesPropertyMetaInformation
        extends CalleesLikePropertyMetaInformation {

    final type Self = SerializationRelatedCallees
}

sealed trait SerializationRelatedCallees extends CalleesLike
        with SerializationRelatedCalleesPropertyMetaInformation {

    override def toString: String = {
        s"SerializationRelatedCallees(size=${this.size})"
    }

    final def key: PropertyKey[SerializationRelatedCallees] = SerializationRelatedCallees.key
}

sealed class SerializationRelatedCalleesImplementation(
        private[properties] val calleesIds: IntMap[IntTrieSet]
) extends SerializationRelatedCallees with CalleesLikeImplementation {
    override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): SerializationRelatedCallees = {
        if (calleesIds.contains(pc) && calleesIds(pc).contains(callee.id)) {
            this
        } else {
            val old = calleesIds.getOrElse(pc, IntTrieSet.empty)
            val newCalleesIds = calleesIds.updated(pc, old + callee.id)
            new SerializationRelatedCalleesImplementation(newCalleesIds)
        }
    }
}

object LowerBoundSerializationRelatedCallees extends SerializationRelatedCallees
        with CalleesLikeLowerBound {
    override def updated(
        pc: Int, callee: DeclaredMethod
    )(implicit declaredMethods: DeclaredMethods): SerializationRelatedCallees = this
}

object NoSerializationRelatedCallees extends SerializationRelatedCalleesImplementation(IntMap.empty)

object NoSerializationRelatedCalleesDueToNotReachableMethod extends SerializationRelatedCallees
    with CalleesLikeNotReachable

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
