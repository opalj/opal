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
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
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
class XTAState(
        override val method:                       DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI],
        // TODO A.B.: is that even a dependee?? we build this ourselves per-method!
        // maybe if other analyses update this, it is one? E.g. InstantiatedTypesAnalysis due to constructors
        private[this] var _ownInstantiatedTypesDependee: EOptionP[DefinedMethod, InstantiatedTypes],

        // dependees for callees ...
        // we need these to find potential new data flows
        // TODO A.B. I'm quite sure we only need callees

        // TODO A.B. Can this depencency become final?
        // TODO A.B. Maybe only if there are no possible additional virtual call sites?
        private[this] var _calleeDependee: EOptionP[DefinedMethod, Callees],

        // TODO A.B. These should be removed once they're final, right?
        // TODO A.B. Can they become final? Probably not!
        private[this] var _calleeInstantiatedTypesDependees:
            Map[DefinedMethod, EOptionP[DefinedMethod, InstantiatedTypes]],

        // TODO A.B.: dependency to InstantiatedTypes of fields it reads --> update own set on update
        // TODO A.B.: store fields it writes --> update types when own types receive an update
) extends CGState {
    private[this] val _virtualCallSites: mutable.LongMap[mutable.Set[CallSiteT]] = mutable.LongMap.empty

    /////////////////////////////////////////////
    //                                         //
    //          instantiated types             //
    //                                         //
    /////////////////////////////////////////////

    // NOTE A.B. "own instantiated types": is the method's set of available

    def updateOwnInstantiatedTypesDependee(
        ownInstantiatedTypesDependee: EOptionP[DefinedMethod, InstantiatedTypes]
    ): Unit = {
        _ownInstantiatedTypesDependee = ownInstantiatedTypesDependee
    }

    def ownInstantiatedTypesDependee(): Option[EOptionP[DefinedMethod, InstantiatedTypes]] = {
        if (_ownInstantiatedTypesDependee.isRefinable)
            Some(_ownInstantiatedTypesDependee)
        else
            None
    }

    def ownInstantiatedTypesUB: UIDSet[ObjectType] = {
        if (_ownInstantiatedTypesDependee.hasUBP)
            _ownInstantiatedTypesDependee.ub.types
        else
            UIDSet.empty
    }

    def newInstantiatedTypes(seenTypes: Int): TraversableOnce[ObjectType] = {
        if (_ownInstantiatedTypesDependee.hasUBP) {
            _ownInstantiatedTypesDependee.ub.dropOldest(seenTypes)
        } else {
            UIDSet.empty
        }
    }

    // Callee stuff

    def calleeDependee(): Option[EOptionP[DefinedMethod, Callees]] = {
        if (_calleeDependee.isRefinable) {
            Some(_calleeDependee)
        } else {
            None
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
        super.hasOpenDependencies ||
          _ownInstantiatedTypesDependee.isRefinable ||
          _calleeInstantiatedTypesDependees.nonEmpty
    }

    override def dependees: List[EOptionP[Entity, Property]] = {
        var dependees = super.dependees

        if (ownInstantiatedTypesDependee().isDefined)
            dependees ::= ownInstantiatedTypesDependee().get

        if (calleeDependee().isDefined)
            dependees ::= calleeDependee().get

        if (_calleeInstantiatedTypesDependees.nonEmpty)
            dependees ++= _calleeInstantiatedTypesDependees.values

        dependees
    }
}
