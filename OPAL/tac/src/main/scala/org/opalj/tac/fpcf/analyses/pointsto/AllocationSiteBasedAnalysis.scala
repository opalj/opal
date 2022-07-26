/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.annotation.switch

import scala.collection.immutable.IntMap

import org.opalj.fpcf.PropertyKey
import org.opalj.br.ObjectType.StringBufferId
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet
import org.opalj.br.ObjectType.ClassId
import org.opalj.br.ObjectType.StringBuilderId
import org.opalj.br.ObjectType.StringId
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSet1
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSites
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.br.fpcf.properties.pointsto.allocationSiteLongToTypeId
import org.opalj.br.fpcf.properties.pointsto.isEmptyArrayAllocationSite
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.classConstPointsToSet
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.exceptionPointsToSets
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergeClassConstsConfigKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergedPointsToSetForType
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergeExceptionsConfigKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergeStringBuilderBufferConfigKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.mergeStringConstsConfigKey
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.stringBufferPointsToSet
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.stringBuilderPointsToSet
import org.opalj.tac.fpcf.analyses.pointsto.AllocationSiteBasedAnalysis.stringConstPointsToSet

trait AllocationSiteBasedAnalysis extends AbstractPointsToBasedAnalysis {

    override protected[this] type ElementType = AllocationSite
    override protected[this] type PointsToSet = AllocationSitePointsToSet

    val mergeStringBuilderBuffer: Boolean =
        project.config.getBoolean(mergeStringBuilderBufferConfigKey)
    val mergeStringConstants: Boolean = project.config.getBoolean(mergeStringConstsConfigKey)
    val mergeClassConstants: Boolean = project.config.getBoolean(mergeClassConstsConfigKey)
    val mergeExceptions: Boolean = project.config.getBoolean(mergeExceptionsConfigKey)

    override protected[this] def createPointsToSet(
        pc:            Int,
        callContext:   ContextType,
        allocatedType: ReferenceType,
        isConstant:    Boolean,
        isEmptyArray:  Boolean       = false
    ): AllocationSitePointsToSet = {
        @inline def createNewPointsToSet(): AllocationSitePointsToSet = {
            val as = allocationSiteToLong(callContext, pc, allocatedType, isEmptyArray)
            AllocationSitePointsToSet1(as, allocatedType)
        }

        (allocatedType.id: @switch) match {
            case StringBuilderId =>
                if (mergeStringBuilderBuffer)
                    stringBuilderPointsToSet
                else
                    createNewPointsToSet()
            case StringBufferId =>
                if (mergeStringBuilderBuffer)
                    stringBufferPointsToSet
                else
                    createNewPointsToSet()
            case StringId =>
                if (mergeStringConstants && isConstant)
                    stringConstPointsToSet
                else
                    createNewPointsToSet()
            case ClassId =>
                if (mergeClassConstants && isConstant)
                    classConstPointsToSet
                else
                    createNewPointsToSet()
            case _ =>
                if (mergeExceptions &&
                    classHierarchy.isSubtypeOf(allocatedType, ObjectType.Throwable)) {
                    val ptsO = exceptionPointsToSets.get(allocatedType.id)
                    if (ptsO.isDefined)
                        ptsO.get
                    else {
                        val newPts = mergedPointsToSetForType(allocatedType)
                        exceptionPointsToSets += allocatedType.id -> newPts
                        newPts
                    }
                } else
                    createNewPointsToSet()
        }
    }

    @inline protected[this] def getTypeOf(element: AllocationSite): ReferenceType = {
        ReferenceType.lookup(allocationSiteLongToTypeId(element))
    }

    @inline protected[this] def getTypeIdOf(element: AllocationSite): Int = {
        allocationSiteLongToTypeId(element)
    }

    @inline protected[this] def isEmptyArray(element: AllocationSite): Boolean = {
        isEmptyArrayAllocationSite(element)
    }

    override protected[this] val pointsToPropertyKey: PropertyKey[AllocationSitePointsToSet] = {
        AllocationSitePointsToSet.key
    }

    @inline protected[this] def emptyPointsToSet: AllocationSitePointsToSet = {
        NoAllocationSites
    }
}

object AllocationSiteBasedAnalysis {

    def mergedPointsToSetForType(r: ReferenceType): AllocationSitePointsToSet =
        AllocationSitePointsToSet1(r.id.toLong << 44 | 0x7FFFFFFFFFFL, r)

    private val configPrefix = "org.opalj.fpcf.analyses.AllocationSiteBasedPointsToAnalysis."
    val mergeStringBuilderBufferConfigKey: String = configPrefix+"mergeStringBuilderBuffer"
    val mergeStringConstsConfigKey: String = configPrefix+"mergeStringConstants"
    val mergeClassConstsConfigKey: String = configPrefix+"mergeClassConstants"
    val mergeExceptionsConfigKey: String = configPrefix+"mergeExceptions"

    // TODO: Create merged pointsTo allocation site
    val stringBuilderPointsToSet: AllocationSitePointsToSet =
        mergedPointsToSetForType(ObjectType.StringBuilder)
    val stringBufferPointsToSet: AllocationSitePointsToSet =
        mergedPointsToSetForType(ObjectType.StringBuffer)
    val stringConstPointsToSet: AllocationSitePointsToSet =
        mergedPointsToSetForType(ObjectType.String)
    val classConstPointsToSet: AllocationSitePointsToSet =
        mergedPointsToSetForType(ObjectType.Class)
    var exceptionPointsToSets: IntMap[AllocationSitePointsToSet] = IntMap()

}