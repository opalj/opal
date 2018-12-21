/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.ValueInformation
import scala.collection.immutable.IntMap

/**
 * Indirect callees existing because of the use of serialization.
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
    protected[this] val calleesIds:          IntMap[IntTrieSet],
    protected[this] val incompleteCallsites: IntTrieSet,
    val parameters:                          IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]]
) extends AbstractCalleesLike with SerializationRelatedCallees

object NoSerializationRelatedCallees
    extends SerializationRelatedCalleesImplementation(IntMap.empty, IntTrieSet.empty, IntMap.empty)

object NoSerializationRelatedCalleesDueToNotReachableMethod
        extends CalleesLikeNotReachable with SerializationRelatedCallees {

    override val parameters: IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]] = IntMap.empty
}

object SerializationRelatedCallees extends SerializationRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[SerializationRelatedCallees] = {
        val name = "opalj.SerializationRelatedCallees"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                    NoSerializationRelatedCalleesDueToNotReachableMethod
                case _ ⇒
                    throw new IllegalStateException(s"No analysis is scheduled for property: $name")
            }
        )
    }
}
