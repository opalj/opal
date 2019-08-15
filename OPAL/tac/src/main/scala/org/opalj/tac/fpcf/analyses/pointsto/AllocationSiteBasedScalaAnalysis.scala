/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.annotation.switch

import scala.collection.immutable.IntMap

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.PropertyKey
import org.opalj.br.fpcf.properties.pointsto.AllocationSitePointsToSetScala
import org.opalj.br.fpcf.properties.pointsto.NoAllocationSitesScala
import org.opalj.br.DeclaredMethod
import org.opalj.br.ObjectType.StringBufferId
import org.opalj.br.ReferenceType
import org.opalj.br.ObjectType.ClassId
import org.opalj.br.fpcf.properties.pointsto.allocationSiteToLong
import org.opalj.br.ObjectType
import org.opalj.br.ObjectType.StringBuilderId
import org.opalj.br.ObjectType.StringId
import org.opalj.br.fpcf.properties.pointsto.AllocationSite

trait AllocationSiteBasedScalaAnalysis extends AbstractPointsToBasedAnalysis {

    override type ElementType = AllocationSite
    override type PointsToSet = AllocationSitePointsToSetScala

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

    override protected[this] def createPointsToSet(
        pc:             Int,
        declaredMethod: DeclaredMethod,
        allocatedType:  ReferenceType,
        isConstant:     Boolean,
        isEmptyArray:   Boolean        = false
    ): AllocationSitePointsToSetScala = {
        @inline def createNewPointsToSet(): AllocationSitePointsToSetScala = {
            val as = allocationSiteToLong(declaredMethod, pc, allocatedType, isEmptyArray)
            new AllocationSitePointsToSetScala(Set(as), UIDSet(allocatedType))
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
                        val newPts = new AllocationSitePointsToSetScala(
                            Set(allocatedType.id.toLong << 39 | 0x3FFFFFFFFFL),
                            UIDSet(allocatedType)
                        )
                        exceptionPointsToSets += allocatedType.id → newPts
                        newPts
                    }
                } else
                    createNewPointsToSet()
        }
    }

    override protected[this] val pointsToPropertyKey: PropertyKey[AllocationSitePointsToSetScala] = {
        AllocationSitePointsToSetScala.key
    }

    override protected[this] def emptyPointsToSet: AllocationSitePointsToSetScala =
        NoAllocationSitesScala
}
