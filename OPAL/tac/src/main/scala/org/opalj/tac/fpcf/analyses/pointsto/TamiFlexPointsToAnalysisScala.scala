/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callers

object AllocationSiteBasedTamiFlexPointsToAnalysisScalaScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        AllocationSitePointsToSet
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        AllocationSitePointsToSet
    )

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new AllocationSiteBasedTamiFlexPointsToAnalysis(p) with AllocationSiteBasedScalaAnalysis
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
}