/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.collection.immutable.IntTrieSet

trait VMReachableMethods extends Property with OrderedProperty {

    protected val reachableMethods: IntTrieSet

    override type Self <: VMReachableMethods

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {
        if (other.reachableMethods != null && !reachableMethods.subsetOf(other.reachableMethods)) {
            throw new IllegalArgumentException(s"$e: illegal refinement of property $other to $this")
        }
    }

    def vmReachableMethods(implicit declaredMethods: DeclaredMethods): Iterator[DeclaredMethod] = {
        reachableMethods.iterator.map(declaredMethods.apply)
    }

    def isMethodReachable(
        declaredMethod: DeclaredMethod
    ): Boolean = {
        reachableMethods contains declaredMethod.id
    }
}

trait VMReachableMethodsFallback extends VMReachableMethods {
    override def vmReachableMethods(
        implicit
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = declaredMethods.declaredMethods

    override def isMethodReachable(
        declaredMethod: DeclaredMethod
    ): Boolean = true

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}
}
