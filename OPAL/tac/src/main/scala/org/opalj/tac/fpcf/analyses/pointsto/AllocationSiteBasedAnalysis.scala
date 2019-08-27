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
import org.opalj.br.DeclaredMethod
import org.opalj.br.fpcf.properties.pointsto.AllocationSite
import org.opalj.br.fpcf.properties.pointsto.allocationSiteLongToTypeId

trait AllocationSiteBasedAnalysis extends AbstractPointsToBasedAnalysis {

    override protected[this] type ElementType = AllocationSite
    override protected[this] type PointsToSet = AllocationSitePointsToSet

    val configPrefix = "org.opalj.fpcf.analyses.AllocationSiteBasedPointsToAnalysis"
    val mergeStringBuilderBuffer: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeStringBuilderBuffer")
    val mergeStringConstants: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeStringConstants")
    val mergeClassConstants: Boolean =
        project.config.getBoolean(s"$configPrefix.mergeClassConstants")
    val mergeExceptions: Boolean = project.config.getBoolean(s"$configPrefix.mergeExceptions")

    // TODO: Create merged pointsTo allocation site
    val stringBuilderPointsToSet = AllocationSitePointsToSet1(StringBuilderId.toLong << 39 | 0x3FFFFFFFFFL, ObjectType.StringBuilder)
    val stringBufferPointsToSet = AllocationSitePointsToSet1(StringBufferId.toLong << 39 | 0x3FFFFFFFFFL, ObjectType.StringBuffer)
    val stringConstPointsToSet = AllocationSitePointsToSet1(StringId.toLong << 39 | 0x3FFFFFFFFFL, ObjectType.String)
    val classConstPointsToSet = AllocationSitePointsToSet1(ClassId.toLong << 39 | 0x3FFFFFFFFFL, ObjectType.Class)
    var exceptionPointsToSets: IntMap[AllocationSitePointsToSet] = IntMap()

    override protected[this] def createPointsToSet(
        pc:             Int,
        declaredMethod: DeclaredMethod,
        allocatedType:  ReferenceType,
        isConstant:     Boolean,
        isEmptyArray:   Boolean        = false
    ): AllocationSitePointsToSet = {
        @inline def createNewPointsToSet(): AllocationSitePointsToSet = {
            val as = allocationSiteToLong(declaredMethod, pc, allocatedType, isEmptyArray)
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
                        val newPts = new AllocationSitePointsToSet1(allocatedType.id.toLong << 39 | 0x3FFFFFFFFFL, allocatedType)
                        exceptionPointsToSets += allocatedType.id → newPts
                        newPts
                    }
                } else
                    createNewPointsToSet()
        }
    }

    override protected[this] def getTypeOf(element: AllocationSite): ReferenceType = {
        ReferenceType.lookup(allocationSiteLongToTypeId(element))
    }

    override protected[this] val pointsToPropertyKey: PropertyKey[AllocationSitePointsToSet] = {
        AllocationSitePointsToSet.key
    }

    override protected[this] def emptyPointsToSet: AllocationSitePointsToSet = {
        NoAllocationSites
    }
}
