/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.collection.immutable.IntTrieSet

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

sealed trait SerializationRelatedCallees extends CalleesLike
    with SerializationRelatedCalleesPropertyMetaInformation {

    override def toString: String = {
        s"SerializationRelatedCallees(size=${this.size})"
    }

    final def key: PropertyKey[SerializationRelatedCallees] = SerializationRelatedCallees.key
}

sealed class SerializationRelatedCalleesImplementation(
        val calleesIds:          IntMap[IntTrieSet],
        val incompleteCallsites: IntTrieSet
) extends AbstractCalleesLike with SerializationRelatedCallees

object NoSerializationRelatedCallees
    extends SerializationRelatedCalleesImplementation(IntMap.empty, IntTrieSet.empty)

object NoSerializationRelatedCalleesDueToNotReachableMethod
    extends CalleesLikeNotReachable with SerializationRelatedCallees

object SerializationRelatedCallees extends SerializationRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[SerializationRelatedCallees] = {
        PropertyKey.create(
            "SerializationRelatedCallees",
            NoSerializationRelatedCalleesDueToNotReachableMethod
        )
    }
}
