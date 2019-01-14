/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cg
package properties

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Entity
import org.opalj.fpcf.PropertyIsNotDerivedByPreviouslyExecutedAnalysis

/**
 * For a given [[org.opalj.br.DeclaredMethod]], and for each call site (represented by the PC), the
 * set of methods that are possible call targets according to a standard analysis of (virtual)
 * calls.
 *
 * @author Florian Kuebler
 */
sealed trait StandardInvokeCalleesPropertyMetaInformation
    extends DirectCalleesPropertyMetaInformation {

    final type Self = StandardInvokeCallees
}

sealed trait StandardInvokeCallees extends CalleesLike
    with StandardInvokeCalleesPropertyMetaInformation {

    override def toString: String = {
        s"StandardInvokeCallees(size=${this.size})"
    }

    final def key: PropertyKey[StandardInvokeCallees] = StandardInvokeCallees.key
}

sealed class StandardInvokeCalleesImplementation(
        val calleesIds:          IntMap[IntTrieSet],
        val incompleteCallSites: IntTrieSet
) extends AbstractCalleesLike with StandardInvokeCallees

object NoStandardInvokeCallees
    extends StandardInvokeCalleesImplementation(IntMap.empty, IntTrieSet.empty)

object NoStandardInvokeCalleesDueToNotReachableMethod
    extends CalleesLikeNotReachable with StandardInvokeCallees

object StandardInvokeCallees extends StandardInvokeCalleesPropertyMetaInformation {

    final val key: PropertyKey[StandardInvokeCallees] = {
        val name = "opalj.StandardInvokeCallees"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒
                    NoStandardInvokeCalleesDueToNotReachableMethod
                case _ ⇒
                    throw new IllegalStateException(s"analysis required for: $name")
            }
        )
    }
}