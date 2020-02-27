/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package pointsto

import org.opalj.br.DeclaredMethod
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedScalaAnalysis
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI

/**
 * TODO
 *
 * @author Dominik Helm
 */
class AllocationSiteBasedPointsToBasedThreadStartScalaAnalysis private[pointsto] (
        final val project:                    SomeProject,
        override final val threadStartMethod: DeclaredMethod
) extends PointsToBasedThreadStartAnalysis with AllocationSiteBasedScalaAnalysis

class AllocationSiteBasedPointsToBasedThreadRelatedCallsScalaAnalysis private[analyses] (
        final val project: SomeProject
) extends PointsToBasedThreadRelatedCallsAnalysis {
    override val createAnalysis: (SomeProject, DeclaredMethod) ⇒ AllocationSiteBasedPointsToBasedThreadStartScalaAnalysis =
        (p, m) ⇒ new AllocationSiteBasedPointsToBasedThreadStartScalaAnalysis(p, m)
}

object AllocationSiteBasedPointsToBasedThreadRelatedCallsScalaAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey, VirtualFormalParametersKey, DefinitionSitesKey)

    override def uses: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers, TACAI)

    override def derivesCollaboratively: Set[PropertyBounds] =
        PropertyBounds.ubs(Callees, Callers)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): AllocationSiteBasedPointsToBasedThreadRelatedCallsScalaAnalysis = {
        val analysis = new AllocationSiteBasedPointsToBasedThreadRelatedCallsScalaAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}