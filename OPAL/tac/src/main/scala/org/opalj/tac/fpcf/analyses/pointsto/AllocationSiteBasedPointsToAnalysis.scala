/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.annotation.switch

import scala.collection.immutable.IntMap

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

    val configPrefix = "org.opalj.fpcf.analyses.AllocationSiteBasedPointsToAnalysis"
    val mergeStringBuilderBuffer: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeStringBuilderBuffer")
    val mergeStringConstants: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeStringConstants")
    val mergeClassConstants: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeClassConstants")
    val mergeExceptions: Boolean = project.config.getBoolean(s"$configPrefix.mergeExceptions")

    // TODO: Create merged pointsTo allocation site
    val stringBuilderPointsToSet = AllocationSitePointsToSet1(StringBuilderId.toLong << 38 | 0x3FFFFFFFFFL, ObjectType.StringBuilder)
    val stringBufferPointsToSet = AllocationSitePointsToSet1(StringBufferId.toLong << 38 | 0x3FFFFFFFFFL, ObjectType.StringBuffer)
    val stringConstPointsToSet = AllocationSitePointsToSet1(StringId.toLong << 38 | 0x3FFFFFFFFFL, ObjectType.String)
    val classConstPointsToSet = AllocationSitePointsToSet1(ClassId.toLong << 38 | 0x3FFFFFFFFFL, ObjectType.Class)
    var exceptionPointsToSets: IntMap[AllocationSitePointsToSet] = IntMap()

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
                if (mergeExceptions &&
                    classHierarchy.isSubtypeOf(allocatedType, ObjectType.Throwable)) {
                    val ptsO = exceptionPointsToSets.get(allocatedType.id)
                    if (ptsO.isDefined)
                        ptsO.get
                    else {
                        val newPts = new AllocationSitePointsToSet1(allocatedType.id.toLong << 38 | 0x3FFFFFFFFFL, allocatedType)
                        exceptionPointsToSets += allocatedType.id → newPts
                        newPts
                    }
                } else
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
