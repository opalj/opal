/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.annotation.switch

import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSites
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet1
import org.opalj.br.ObjectType.ClassId
import org.opalj.br.ObjectType.StringBufferId
import org.opalj.br.ObjectType.StringBuilderId
import org.opalj.br.ObjectType.StringId
import org.opalj.tac.fpcf.properties.TACAI

class AllocationSiteBasedPointsToAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractPointsToAnalysis[Long, AllocationSitePointsToSet] {

    val mergeStringBuilderBuffer = project.config.getBoolean("org.opalj.fpcf.analyses.AllocationSiteBasedPointsToAnalysis.mergeStringBuilderBuffer")
    val mergeStringConstants = project.config.getBoolean("org.opalj.fpcf.analyses.AllocationSiteBasedPointsToAnalysis.mergeStringConstants")
    val mergeClassConstants = project.config.getBoolean("org.opalj.fpcf.analyses.AllocationSiteBasedPointsToAnalysis.mergeClassConstants")

    val stringBuilderPointsToSet = AllocationSitePointsToSet1(StringBuilderId.toLong << 38 | 0x3FFFFFFFFFL, ObjectType.StringBuilder)
    val stringBufferPointsToSet = AllocationSitePointsToSet1(StringBufferId.toLong << 38 | 0x3FFFFFFFFFL, ObjectType.StringBuffer)
    val stringConstPointsToSet = AllocationSitePointsToSet1(StringId.toLong << 38 | 0x3FFFFFFFFFL, ObjectType.String)
    val classConstPointsToSet = AllocationSitePointsToSet1(ClassId.toLong << 38 | 0x3FFFFFFFFFL, ObjectType.Class)

    override def createPointsToSet(
        pc:             Int,
        declaredMethod: DeclaredMethod,
        allocatedType:  ReferenceType,
        isConstant:     Boolean
    ): AllocationSitePointsToSet = {
        @inline def createNewPointsToSet(): AllocationSitePointsToSet = {
            val as = allocationSiteToLong(declaredMethod, pc, allocatedType)
            new AllocationSitePointsToSet1(as, allocatedType)
        }

        (allocatedType.id: @switch) match {
            case StringBuilderId ⇒
                if (mergeStringBuilderBuffer)
                    stringBuilderPointsToSet
                else
                    createNewPointsToSet()
            case StringBufferId ⇒
                if (mergeStringBuilderBuffer)
                    stringBufferPointsToSet
                else
                    createNewPointsToSet()
            case StringId ⇒
                if (mergeStringConstants && isConstant)
                    stringConstPointsToSet
                else
                    createNewPointsToSet()
            case ClassId ⇒
                if (mergeClassConstants && isConstant)
                    classConstPointsToSet
                else
                    createNewPointsToSet()
            case _ ⇒
                createNewPointsToSet()
        }
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
