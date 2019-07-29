/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.annotation.switch

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.ObjectType
import org.opalj.br.ObjectType.ClassId
import org.opalj.br.ObjectType.StringBufferId
import org.opalj.br.ObjectType.StringBuilderId
import org.opalj.br.ObjectType.StringId
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSetScala
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSitesScala
import org.opalj.tac.fpcf.properties.TACAI

class AllocationSiteBasedPointsToScalaAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractPointsToAnalysis[AllocationSite, AllocationSitePointsToSetScala] {

    val configPrefix = "org.opalj.fpcf.analyses.AllocationSiteBasedPointsToAnalysis"
    val mergeStringBuilderBuffer: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeStringBuilderBuffer")
    val mergeStringConstants: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeStringConstants")
    val mergeClassConstants: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeClassConstants")
    val mergeExceptions: Boolean = project.config.getBoolean(s"$configPrefix.mergeExceptions")

    // TODO: Create merged pointsTo allocation site
    val stringBuilderPointsToSet = AllocationSitePointsToSetScala(Set(StringBuilderId.toLong << 39 | 0x3FFFFFFFFFL), UIDSet(ObjectType.StringBuilder))
    val stringBufferPointsToSet = AllocationSitePointsToSetScala(Set(StringBufferId.toLong << 39 | 0x3FFFFFFFFFL), UIDSet(ObjectType.StringBuffer))
    val stringConstPointsToSet = AllocationSitePointsToSetScala(Set(StringId.toLong << 39 | 0x3FFFFFFFFFL), UIDSet(ObjectType.String))
    val classConstPointsToSet = AllocationSitePointsToSetScala(Set(ClassId.toLong << 39 | 0x3FFFFFFFFFL), UIDSet(ObjectType.Class))
    var exceptionPointsToSets: IntMap[AllocationSitePointsToSetScala] = IntMap()

    override def createPointsToSet(
        pc:             Int,
        declaredMethod: DeclaredMethod,
        allocatedType:  ReferenceType,
        isConstant:     Boolean,
        isEmptyArray:   Boolean        = false
    ): AllocationSitePointsToSetScala = {
        @inline def createNewPointsToSet(): AllocationSitePointsToSetScala = {
            val as = allocationSiteToLong(declaredMethod, pc, allocatedType, isEmptyArray)
            AllocationSitePointsToSetScala(Set(as), UIDSet(allocatedType))
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
                        val newPts = AllocationSitePointsToSetScala(Set(allocatedType.id.toLong << 39 | 0x3FFFFFFFFFL), UIDSet(allocatedType))
                        exceptionPointsToSets += allocatedType.id → newPts
                        newPts
                    }
                } else
                    createNewPointsToSet()
        }
    }

    override protected val pointsToPropertyKey: PropertyKey[AllocationSitePointsToSetScala] = {
        AllocationSitePointsToSetScala.key
    }

    override protected def emptyPointsToSet: AllocationSitePointsToSetScala = {
        NoAllocationSitesScala
    }
}

object AllocationSiteBasedPointsToScalaAnalysisScheduler extends FPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        AllocationSitePointsToSetScala,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = {
        Set(PropertyBounds.ub(AllocationSitePointsToSetScala))
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        null
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def register(
        p: SomeProject, ps: PropertyStore, unused: Null
    ): AllocationSiteBasedPointsToScalaAnalysis = {
        val analysis = new AllocationSiteBasedPointsToScalaAnalysis(p)
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
