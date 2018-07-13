/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet

sealed trait VMReachableFinalizersMetaInformation extends PropertyMetaInformation {
    final type Self = VMReachableFinalizers
}

/**
 * TODO
 * @author Florian Kuebler
 */
sealed class VMReachableFinalizers(override protected val reachableMethods: IntTrieSet)
    extends VMReachableMethods with VMReachableFinalizersMetaInformation {

    override def key: PropertyKey[VMReachableFinalizers] = VMReachableFinalizers.key

    override def toString: String = s"VMReachableFinalizers(size=${reachableMethods.size})"
}

object VMReachableFinalizersFallback extends VMReachableFinalizers(reachableMethods = null)
    with VMReachableMethodsFallback

object VMReachableFinalizers extends VMReachableFinalizersMetaInformation {
    final val key: PropertyKey[VMReachableFinalizers] = {
        PropertyKey.create(
            "VMReachableFinalizers",
            (ps: PropertyStore, _: FallbackReason, p: SomeProject) ⇒ VMReachableFinalizersFallback,
            (_: PropertyStore, eps: EPS[SomeProject, VMReachableFinalizers]) ⇒ eps.ub,
            (_: PropertyStore, _: SomeProject) ⇒ None
        )
    }
}