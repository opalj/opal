/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package cg
package properties

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.OrderedProperty
import org.opalj.br.analyses.DeclaredMethods

/**
 * @author Florian Kübler
 */
trait VMReachableMethods extends OrderedProperty {

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

    def isMethodReachable(declaredMethod: DeclaredMethod): Boolean = {
        reachableMethods contains declaredMethod.id
    }
}

trait VMReachableMethodsFallback extends VMReachableMethods {

    override def vmReachableMethods(
        implicit
        declaredMethods: DeclaredMethods
    ): Iterator[DeclaredMethod] = {
        declaredMethods.declaredMethods
    }

    override def isMethodReachable(declaredMethod: DeclaredMethod): Boolean = true

    override def checkIsEqualOrBetterThan(e: Entity, other: Self): Unit = {}
}
