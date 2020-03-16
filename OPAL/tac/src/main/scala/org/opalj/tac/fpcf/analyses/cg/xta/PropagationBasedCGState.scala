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

    private[this] val _virtualCallSites = new java.util.HashMap[Long, java.util.HashSet[CallSiteT]]

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
        val values = _instantiatedTypesDependeeMap.values().iterator()
        var exists = false
        while (!exists && values.hasNext) {
            val its = instantiatedTypes(values.next().e)
            exists |= its.contains(tpe)
        }

        return exists
        //        _instantiatedTypesDependeeMap.keySet().iterator().exists(instantiatedTypes(_).contains(tpe))
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

    override def hasNonFinalCallSite: Boolean = !_virtualCallSites.isEmpty

    def addVirtualCallSite(objectType: ObjectType, callSite: CallSiteT): Unit = {
        val id = objectType.id.toLong
        val oldValOpt = _virtualCallSites.get(id)
        if (oldValOpt ne null)
            oldValOpt.add(callSite)
        else {
            val hs = new java.util.HashSet[CallSiteT]()
            hs.add(callSite)
            _virtualCallSites.put(objectType.id.toLong, hs)
        }
    }

    def getVirtualCallSites(objectType: ObjectType): java.util.HashSet[CallSiteT] = {
        _virtualCallSites.getOrDefault(objectType.id.toLong, new java.util.HashSet[CallSiteT])
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
        super.hasOpenDependencies || {
            var exists = false
            val itr = _instantiatedTypesDependeeMap.values().iterator()
            while (!exists && itr.hasNext) {
                val elem = itr.next()
                exists |= elem.isRefinable
            }
            exists
        }
    }

    override def dependees: Set[SomeEOptionP] = {
        super.dependees ++ _instantiatedTypesDependeeMap.values().asScala.toSet
    }
}