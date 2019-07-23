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
 * Manages the state used by the [[PropagationBasedCallGraphAnalysis]].
 *
 * @author Andreas Bauer
 */
class PropagationBasedCGState(
        override val method:                       DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI],
        _instantiatedTypesDependees:               Iterable[EOptionP[SetEntity, InstantiatedTypes]]
) extends CGState {

    private[this] val _instantiatedTypesDependeeMap: mutable.Map[SetEntity, EOptionP[SetEntity, InstantiatedTypes]] = mutable.Map.empty

    for (dependee ← _instantiatedTypesDependees) {
        _instantiatedTypesDependeeMap.update(dependee.e, dependee)
    }

    private[this] val _virtualCallSites: mutable.LongMap[mutable.Set[CallSiteT]] = mutable.LongMap.empty

    /////////////////////////////////////////////
    //                                         //
    //          instantiated types             //
    //                                         //
    /////////////////////////////////////////////

    def updateInstantiatedTypesDependee(
        instantiatedTypesDependee: EOptionP[SetEntity, InstantiatedTypes]
    ): Unit = {
        _instantiatedTypesDependeeMap.update(instantiatedTypesDependee.e, instantiatedTypesDependee)
    }

    def instantiatedTypes(setEntity: SetEntity): UIDSet[ReferenceType] = {
        val typeDependee = _instantiatedTypesDependeeMap(setEntity)
        if (typeDependee.hasUBP)
            typeDependee.ub.types
        else
            UIDSet.empty
    }

    def instantiatedTypesContains(tpe: ReferenceType): Boolean = {
        _instantiatedTypesDependeeMap.keys.exists(instantiatedTypes(_).contains(tpe))
    }

    def newInstantiatedTypes(setEntity: SetEntity, seenTypes: Int): TraversableOnce[ReferenceType] = {
        val typeDependee = _instantiatedTypesDependeeMap(setEntity)
        if (typeDependee.hasUBP) {
            typeDependee.ub.dropOldest(seenTypes)
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
        _instantiatedTypesDependeeMap.exists(_._2.isRefinable) || super.hasOpenDependencies
    }

    override def dependees: List[EOptionP[Entity, Property]] = {
        _instantiatedTypesDependeeMap.values ++: super.dependees
    }
}