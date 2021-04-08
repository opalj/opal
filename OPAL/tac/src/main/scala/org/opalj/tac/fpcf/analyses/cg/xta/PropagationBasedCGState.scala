/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEOptionP
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.tac.fpcf.properties.TACAI

import scala.collection.mutable

import scala.collection.JavaConverters._

/**
 * Manages the state of each method analyzed by [[PropagationBasedCallGraphAnalysis]].
 *
 * @author Andreas Bauer
 */
class PropagationBasedCGState(
        override val method:                       DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI],
        _instantiatedTypesDependees:               Iterable[EOptionP[TypeSetEntity, InstantiatedTypes]]
) extends CGState {

    private[this] val _instantiatedTypesDependeeMap = new java.util.HashMap[TypeSetEntity, EOptionP[TypeSetEntity, InstantiatedTypes]]()

    for (dependee ← _instantiatedTypesDependees) {
        _instantiatedTypesDependeeMap.put(dependee.e, dependee)
    }

    private[this] val _virtualCallSites: mutable.LongMap[mutable.Set[CallSiteT]] = mutable.LongMap.empty

    /////////////////////////////////////////////
    //                                         //
    //          instantiated types             //
    //                                         //
    /////////////////////////////////////////////

    def updateInstantiatedTypesDependee(
        instantiatedTypesDependee: EOptionP[TypeSetEntity, InstantiatedTypes]
    ): Unit = {
        _instantiatedTypesDependeeMap.put(instantiatedTypesDependee.e, instantiatedTypesDependee)
    }

    def instantiatedTypes(typeSetEntity: TypeSetEntity): UIDSet[ReferenceType] = {
        val typeDependee = _instantiatedTypesDependeeMap.get(typeSetEntity)
        if (typeDependee.hasUBP)
            typeDependee.ub.types
        else
            UIDSet.empty
    }

    def instantiatedTypesContains(tpe: ReferenceType): Boolean = {
        _instantiatedTypesDependeeMap.values().iterator().asScala.exists { eOptP ⇒
            instantiatedTypes(eOptP.e).contains(tpe)
        }
    }

    def newInstantiatedTypes(typeSetEntity: TypeSetEntity, seenTypes: Int): TraversableOnce[ReferenceType] = {
        val typeDependee = _instantiatedTypesDependeeMap.get(typeSetEntity)
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
        _virtualCallSites.remove(instantiatedType.id.toLong)
    }

    /////////////////////////////////////////////
    //                                         //
    //      general dependency management      //
    //                                         //
    /////////////////////////////////////////////

    override def hasOpenDependencies: Boolean = {
        super.hasOpenDependencies ||
            _instantiatedTypesDependeeMap.values().iterator().asScala.exists(_.isRefinable)
    }

    override def dependees: Set[SomeEOptionP] = {
        super.dependees ++ _instantiatedTypesDependeeMap.values().asScala
    }
}