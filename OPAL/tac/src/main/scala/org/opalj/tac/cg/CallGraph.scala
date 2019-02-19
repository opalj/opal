/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.collection.IntIterator
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.cg.properties.NoCallers

/**
 * The proxy class for all call-graph related properties.
 * All information will be queried in the property store ([[PropertyStore]]), therefore,
 * all values for [[Callees]] and [[CallersProperty]] in the property store must be final,
 * when instantiating this class.
 *
 * @author Florian Kuebler
 */
class CallGraph private[cg] ()(implicit ps: PropertyStore, declaredMethods: DeclaredMethods) {
    assert(ps.entities(_.pk == Callees.key).forall(ps(_, Callees.key).isFinal))
    assert(ps.entities(_.pk == CallersProperty.key).forall(ps(_, CallersProperty.key).isFinal))

    def calleesOf(m: DeclaredMethod, pc: Int): Iterator[DeclaredMethod] = {
        ps(m, Callees.key).ub.callees(pc)
    }

    def calleesOf(m: DeclaredMethod): Iterator[(Int, Iterator[DeclaredMethod])] = {
        ps(m, Callees.key).ub.callSites().toIterator
    }

    def calleesPropertyOf(m: DeclaredMethod): Callees = {
        ps(m, Callees.key).ub
    }

    def directCalleesOf(m: DeclaredMethod, pc: Int): Iterator[DeclaredMethod] = {
        ps(m, Callees.key).ub.directCallees(pc)
    }

    def indirectCalleesOf(m: DeclaredMethod, pc: Int): Iterator[DeclaredMethod] = {
        ps(m, Callees.key).ub.indirectCallees(pc)
    }

    def isIncompleteCallSiteOf(m: DeclaredMethod, pc: Int): Boolean = {
        ps(m, Callees.key).ub.isIncompleteCallSite(pc)
    }

    def incompleteCallSitesOf(m: DeclaredMethod): IntIterator = {
        ps(m, Callees.key).ub.incompleteCallSites
    }

    def callersOf(m: DeclaredMethod): TraversableOnce[(DeclaredMethod, Int)] = {
        ps(m, CallersProperty.key).ub.callers
    }

    def callersPropertyOf(m: DeclaredMethod): CallersProperty = {
        ps(m, CallersProperty.key).ub
    }

    def hasVMLevelCaller(m: DeclaredMethod): Boolean = {
        ps(m, CallersProperty.key).ub.hasVMLevelCallers
    }

    def hasCallersWithUnknownContext(m: DeclaredMethod): Boolean = {
        ps(m, CallersProperty.key).ub.hasCallersWithUnknownContext
    }

    def reachableMethods(): Iterator[DeclaredMethod] = {
        val callersProperties = ps.entities(CallersProperty.key)

        callersProperties.filterNot(_.ub eq NoCallers).map(_.e.asInstanceOf[DeclaredMethod])
    }

    lazy val numEdges: Int = {
        val callers = ps.entities(CallersProperty.key).map(_.ub.callers)
        callers.map(_.size).sum
    }
}