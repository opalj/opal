/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.analyses.L1ThrownExceptionsAnalysis
import org.opalj.br.fpcf.analyses.ThrownExceptionsAnalysisScheduler
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.cg.CallGraphKey

/**
 * Factory and runner for the [[L1ThrownExceptionsAnalysis]].
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object EagerL1ThrownExceptionsAnalysis
    extends ThrownExceptionsAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    /**
     * Eagerly schedules the computation of the thrown exceptions for all methods with bodies;
     * in general, the analysis is expected to be registered as a lazy computation.
     */
    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1ThrownExceptionsAnalysis(p)
        val cg = p.get(CallGraphKey)
        ps.scheduleEagerComputationsForEntities(cg.reachableMethods())(analysis.determineThrownExceptions)
        analysis
    }
}
