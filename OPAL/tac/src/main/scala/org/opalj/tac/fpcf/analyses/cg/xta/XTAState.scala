/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.tac.fpcf.properties.TACAI

import scala.collection.mutable

/**
 * Manages the state used by the [[XTACallGraphAnalysis]].
 *
 * @author Andreas Bauer
 */
// TODO AB code duplication: this is very similar to the RTAState (except for the entity of the type property).
class XTAState(
        override val method:                          DefinedMethod,
        override protected[this] var _tacDependee:    EOptionP[Method, TACAI],
        private[this] var _instantiatedTypesDependee: EOptionP[DefinedMethod, InstantiatedTypes]
) extends CGState {
    private[this] val _virtualCallSites: mutable.LongMap[mutable.Set[CallSiteT]] = mutable.LongMap.empty

    /////////////////////////////////////////////
    //                                         //
    //          instantiated types             //
    //                                         //
    /////////////////////////////////////////////

    def updateInstantiatedTypesDependee(
        instantiatedTypesDependee: EOptionP[DefinedMethod, InstantiatedTypes]
    ): Unit = {
        _instantiatedTypesDependee = instantiatedTypesDependee
    }

    def instantiatedTypesDependee(): Option[EOptionP[DefinedMethod, InstantiatedTypes]] = {
        if (_instantiatedTypesDependee.isRefinable)
            Some(_instantiatedTypesDependee)
        else
            None
    }

    def instantiatedTypesUB: UIDSet[ReferenceType] = {
        if (_instantiatedTypesDependee.hasUBP)
            _instantiatedTypesDependee.ub.types
        else
            UIDSet.empty
    }

    def newInstantiatedTypes(seenTypes: Int): TraversableOnce[ReferenceType] = {
        if (_instantiatedTypesDependee.hasUBP) {
            _instantiatedTypesDependee.ub.dropOldest(seenTypes)
        } else {
            UIDSet.empty
        }
    }

    /////////////////////////////////////////////
    //                                         //
    //          virtual call sites             //
    //                                         //
    /////////////////////////////////////////////

    override def hasNonFinalCallSite: Boolean = _virtualCallSites.nonEmpty

    def addVirtualCallSite(objectType: ObjectType, callSite: CallSiteT): Unit = {
        val oldValOpt = _virtualCallSites.get(objectType.id.toLong)
        if (oldValOpt.isDefined)
            oldValOpt.get += callSite
        else {
            _virtualCallSites += (objectType.id.toLong → mutable.Set(callSite))
        }
    }

    def getVirtualCallSites(objectType: ObjectType): scala.collection.Set[CallSiteT] = {
        _virtualCallSites.getOrElse(objectType.id.toLong, scala.collection.Set.empty)
    }

    def removeCallSite(instantiatedType: ObjectType): Unit = {
        _virtualCallSites -= instantiatedType.id.toLong
    }

    /////////////////////////////////////////////
    //                                         //
    //      general dependency management      //
    //                                         //
    /////////////////////////////////////////////

    override def hasOpenDependencies: Boolean = {
        _instantiatedTypesDependee.isRefinable || super.hasOpenDependencies
    }

    override def dependees: List[EOptionP[Entity, Property]] = {
        if (instantiatedTypesDependee().isDefined)
            instantiatedTypesDependee().get :: super.dependees
        else
            super.dependees
    }
}