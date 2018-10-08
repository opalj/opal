/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.value.KnownTypedValue

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
        val calleesIds:          IntMap[IntTrieSet],
        val incompleteCallsites: IntTrieSet,
        val parameters:          IntMap[Map[DeclaredMethod, Seq[Option[(KnownTypedValue, IntTrieSet)]]]]
) extends AbstractCalleesLike with SerializationRelatedCallees

object NoSerializationRelatedCallees
    extends SerializationRelatedCalleesImplementation(IntMap.empty, IntTrieSet.empty, IntMap.empty)

object NoSerializationRelatedCalleesDueToNotReachableMethod
    extends CalleesLikeNotReachable with SerializationRelatedCallees {

    override val parameters: IntMap[Map[DeclaredMethod, Seq[Option[(KnownTypedValue, IntTrieSet)]]]] = IntMap.empty
}

object SerializationRelatedCallees extends SerializationRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[SerializationRelatedCallees] = {
        PropertyKey.forSimpleProperty(
            "SerializationRelatedCallees",
            NoSerializationRelatedCalleesDueToNotReachableMethod
        )
    }
}
