/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSites
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.TACAI

class AllocationSiteBasedPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractPointsToAnalysis[AllocationSitePointsToSet] {
    override def createPointsToSet(
        pc: Int, declaredMethod: DeclaredMethod, allocatedType: ObjectType
    ): AllocationSitePointsToSet = {
        val as = allocationSiteToLong(declaredMethod, pc, allocatedType)
        AllocationSitePointsToSet(as, allocatedType)
    }

    override protected val pointsToPropertyKey: PropertyKey[AllocationSitePointsToSet] = {
        AllocationSitePointsToSet.key
    }

    override protected def emptyPointsToSet: AllocationSitePointsToSet = {
        NoAllocationSites
    }
}

object AllocationSiteBasedPointsToAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        AllocationSitePointsToSet,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = {
        Set(PropertyBounds.ub(AllocationSitePointsToSet))
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): AllocationSiteBasedPointsToAnalysis = {
        val analysis = new AllocationSiteBasedPointsToAnalysis(p)
        // register the analysis for initial values for callers (i.e. methods becoming reachable)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def afterPhaseScheduling(ps: PropertyStore, analysis: FPCFAnalysis): Unit = {}

    override def afterPhaseCompletion(
        p:        SomeProject,
        ps:       PropertyStore,
        analysis: FPCFAnalysis
    ): Unit = {}

    override def triggeredBy: PropertyKind = Callers
}
