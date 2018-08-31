/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.collection.immutable.IntTrieSet

import scala.collection.immutable.IntMap

/**
 * Indirect callees existing because of the use of reflection.
 *
 * @author Dominik Helm
 */
sealed trait ReflectionRelatedCalleesPropertyMetaInformation
    extends IndirectCalleesPropertyMetaInformation {

    final type Self = ReflectionRelatedCallees
}

sealed trait ReflectionRelatedCallees
    extends CalleesLike with ReflectionRelatedCalleesPropertyMetaInformation {

    override def toString: String = {
        s"ReflectionRelatedCallees(size=${this.size})"
    }

    final def key: PropertyKey[ReflectionRelatedCallees] = ReflectionRelatedCallees.key
}

sealed class ReflectionRelatedCalleesImplementation(
        val calleesIds:          IntMap[IntTrieSet],
        val incompleteCallsites: IntTrieSet
) extends AbstractCalleesLike with ReflectionRelatedCallees

object NoReflectionRelatedCallees
    extends ReflectionRelatedCalleesImplementation(IntMap.empty, IntTrieSet.empty)

object NoReflectionRelatedCalleesDueToNotReachableMethod
    extends CalleesLikeNotReachable with ReflectionRelatedCallees

object ReflectionRelatedCallees extends ReflectionRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[ReflectionRelatedCallees] = {
        PropertyKey.create(
            "ReflectionRelatedCallees",
            NoReflectionRelatedCalleesDueToNotReachableMethod
        )
    }
}
