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
 * Indirect callees existing because of the use of reflection.
 *
 * @author Dominik Helm
 */
sealed trait ReflectionRelatedCalleesPropertyMetaInformation
    extends IndirectCalleesPropertyMetaInformation {

    final type Self = ReflectionRelatedCallees
}

sealed trait ReflectionRelatedCallees
    extends IndirectCallees with ReflectionRelatedCalleesPropertyMetaInformation {

    override def toString: String = {
        s"ReflectionRelatedCallees(size=${this.size})"
    }

    final def key: PropertyKey[ReflectionRelatedCallees] = ReflectionRelatedCallees.key
}

sealed class ReflectionRelatedCalleesImplementation(
        protected[this] val calleesIds:          IntMap[IntTrieSet],
        protected[this] val incompleteCallsites: IntTrieSet,
        val parameters:                          IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]]
) extends AbstractCalleesLike with ReflectionRelatedCallees

object NoReflectionRelatedCallees
    extends ReflectionRelatedCalleesImplementation(IntMap.empty, IntTrieSet.empty, IntMap.empty)

object NoReflectionRelatedCalleesDueToNotReachableMethod
    extends CalleesLikeNotReachable with ReflectionRelatedCallees {

    override val parameters: IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]] = IntMap.empty
}

object ReflectionRelatedCallees extends ReflectionRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[ReflectionRelatedCallees] = {
        PropertyKey.forSimpleProperty(
            "ReflectionRelatedCallees",
            NoReflectionRelatedCalleesDueToNotReachableMethod
        )
    }
}
