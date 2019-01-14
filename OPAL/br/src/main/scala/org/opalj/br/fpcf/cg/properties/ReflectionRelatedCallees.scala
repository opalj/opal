/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cg
package properties

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.value.ValueInformation

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
        protected[this] val calleesIds: IntMap[IntTrieSet],
        val incompleteCallSites:        IntTrieSet,
        val parameters:                 IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]]
) extends AbstractCalleesLike with ReflectionRelatedCallees

object NoReflectionRelatedCallees
    extends ReflectionRelatedCalleesImplementation(IntMap.empty, IntTrieSet.empty, IntMap.empty)

object NoReflectionRelatedCalleesDueToNotReachableMethod
    extends CalleesLikeNotReachable with ReflectionRelatedCallees {

    override val parameters: IntMap[Map[DeclaredMethod, Seq[Option[(ValueInformation, IntTrieSet)]]]] = IntMap.empty
}

object ReflectionRelatedCallees extends ReflectionRelatedCalleesPropertyMetaInformation {

    final val key: PropertyKey[ReflectionRelatedCallees] = {
        val name = "opalj.ReflectionRelatedCallees"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                    NoReflectionRelatedCalleesDueToNotReachableMethod
                case _ ⇒
                    throw new IllegalStateException(s"analysis required for property: $name")
            }
        )
    }
}
