/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg

import org.opalj.fpcf.EUBP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.NoCallers

/**
 * The proxy class for all call-graph related properties.
 * All information will be queried from the property store ([[org.opalj.fpcf.PropertyStore]]),
 * therefore, all values for [[Callees]] and
 * [[Callers]] in the property store must be final when
 * instantiating this class.
 *
 * @author Florian Kuebler
 */
class CallGraph private[cg] ()(implicit ps: PropertyStore, typeProvider: TypeProvider) {
    assert(ps.entities(_.pk == Callees.key).forall(ps(_, Callees.key).isFinal))
    assert(ps.entities(_.pk == Callers.key).forall(ps(_, Callers.key).isFinal))

    def calleesOf(m: DeclaredMethod, pc: Int): Iterator[Context] = {
        val callees = ps(m, Callees.key).ub
        callees.callerContexts.flatMap(callees.callees(_, pc))
    }

    def calleesOf(m: DeclaredMethod): Iterator[(Int, Iterator[Context])] = {
        // IMPROVE: avoid inefficient boxing operations
        val callees = ps(m, Callees.key).ub
        callees.callerContexts.flatMap(callees.callSites(_).iterator)
    }

    def calleesPropertyOf(m: DeclaredMethod): Callees = {
        ps(m, Callees.key).ub
    }

    def directCalleesOf(m: DeclaredMethod, pc: Int): Iterator[Context] = {
        val callees = ps(m, Callees.key).ub
        callees.callerContexts.flatMap(callees.directCallees(_, pc))
    }

    def indirectCalleesOf(m: DeclaredMethod, pc: Int): Iterator[Context] = {
        val callees = ps(m, Callees.key).ub
        callees.callerContexts.flatMap(callees.indirectCallees(_, pc))
    }

    def isIncompleteCallSiteOf(m: DeclaredMethod, pc: Int): Boolean = {
        val callees = ps(m, Callees.key).ub
        callees.callerContexts.exists(callees.isIncompleteCallSite(_, pc))
    }

    def incompleteCallSitesOf(m: DeclaredMethod): Iterator[Int] = {
        val callees = ps(m, Callees.key).ub
        callees.callerContexts.flatMap(callees.incompleteCallSites(_))
    }

    /**
     * For the given method it returns all callers, including the pc of the call-site and a flag,
     * indicating whether the call was direct (true) or indirect (false).
     */
    def callersOf(m: DeclaredMethod): IterableOnce[(DeclaredMethod, Int, Boolean)] = {
        ps(m, Callers.key).ub.callers(m)
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

    def reachableMethods(): Iterator[Context] = {
        val callersProperties = ps.entities(Callers.key)
        callersProperties.flatMap {
            case EUBP(m: DeclaredMethod, ub: Callers) if ub ne NoCallers =>
                ub.calleeContexts(m).iterator.map { context =>
                    if (context.hasContext) context
                    else typeProvider.newContext(m)
                }
            case _ =>
                Iterator.empty
        }
    }

    lazy val numEdges: Int = {
        ps.entities(Callers.key).map { cs => cs.ub.callers(cs.e.asInstanceOf[DeclaredMethod]).iterator.size }.sum
    }
}