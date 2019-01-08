/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package cg
package properties

import org.opalj.collection.immutable.IntTrieSet

sealed trait VMReachableFinalizersMetaInformation extends PropertyMetaInformation {
    final type Self = VMReachableFinalizers
}

/**
 * TODO
 * @author Florian Kübler
 */
sealed class VMReachableFinalizers(
    protected val reachableMethods: IntTrieSet
    )    extends VMReachableMethods 
    with VMReachableFinalizersMetaInformation {

    override def key: PropertyKey[VMReachableFinalizers] = VMReachableFinalizers.key

    override def toString: String = s"VMReachableFinalizers(size=${reachableMethods.size})"
}

object NoVMReachableFinalizers extends VMReachableFinalizers(reachableMethods = IntTrieSet.empty)

object VMReachableFinalizers extends VMReachableFinalizersMetaInformation {
    final val key: PropertyKey[VMReachableFinalizers] = {
        val name = "opalj.VMReachableFinalizers"
        PropertyKey.create(
            name,
            (_: PropertyStore, reason: FallbackReason, _: Entity) ⇒ reason match {
                case PropertyIsNotDerivedByPreviouslyExecutedAnalysis ⇒ NoVMReachableFinalizers
                case _ ⇒
                    throw new IllegalStateException(s"No analysis is scheduled for property: $name")
            }
        )
    }
}