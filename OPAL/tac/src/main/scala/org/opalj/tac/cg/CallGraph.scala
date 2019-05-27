/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.collection.IntIterator
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.Callers
import org.opalj.br.fpcf.cg.properties.NoCallers

/**
 * The proxy class for all call-graph related properties.
 * All information will be queried from the property store ([[org.opalj.fpcf.PropertyStore]]),
 * therefore, all values for [[org.opalj.br.fpcf.cg.properties.Callees]] and
 * [[org.opalj.br.fpcf.cg.properties.Callers]] in the property store must be final when
 * instantiating this class.
 *
 * @author Florian Kuebler
 */
class CallGraph private[cg] ()(implicit ps: PropertyStore, declaredMethods: DeclaredMethods) {
    assert(ps.entities(_.pk == Callees.key).forall(ps(_, Callees.key).isFinal))
    assert(ps.entities(_.pk == Callers.key).forall(ps(_, Callers.key).isFinal))

    def calleesOf(m: DeclaredMethod, pc: Int): Iterator[DeclaredMethod] = {
        ps(m, Callees.key).ub.callees(pc)
    }

    def calleesOf(m: DeclaredMethod): Iterator[(Int, Iterator[DeclaredMethod])] = {
        // IMPROVE: avoid inefficient boxing operations
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

    /**
     * For the given method it returns all callers, including the pc of the call-site and a flag,
     * indicating whether the call was direct (true) or indirect (false).
     */
    def callersOf(m: DeclaredMethod): TraversableOnce[(DeclaredMethod, Int, Boolean)] = {
        ps(m, Callers.key).ub.callers
    }

    def callersPropertyOf(m: DeclaredMethod): Callers = {
        ps(m, Callers.key).ub
    }

    def hasVMLevelCaller(m: DeclaredMethod): Boolean = {
        ps(m, Callers.key).ub.hasVMLevelCallers
    }

    def hasCallersWithUnknownContext(m: DeclaredMethod): Boolean = {
        ps(m, Callers.key).ub.hasCallersWithUnknownContext
    }

    def reachableMethods(): Iterator[DeclaredMethod] = {
        val callersProperties = ps.entities(Callers.key)
        callersProperties.collect {
            case EUBP(dm: DeclaredMethod, ub: Callers) if ub ne NoCallers ⇒ dm
        }
    }

    lazy val numEdges: Int = {
        val callers = ps.entities(Callers.key).map(_.ub.callers)
        // IMPROVE: efficiently calculate the sum
        callers.map(_.size).sum
    }
}