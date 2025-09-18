/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEOptionP

final class ConfiguredNativeMethodsTypePropagationState[ContextType <: Context](
    val callContext:                                 ContextType,
    val configurationData:                           Array[EntityAssignment],
    val typeSetEntity:                               TypeSetEntity,
    private[this] var _ownInstantiatedTypesDependee: EOptionP[TypeSetEntity, InstantiatedTypes]
) extends BaseAnalysisState {

    /////////////////////////////////////////////
    //                                         //
    //           own types (method)            //
    //                                         //
    /////////////////////////////////////////////

    def updateOwnInstantiatedTypesDependee(eps: EOptionP[TypeSetEntity, InstantiatedTypes]): Unit = {
        _ownInstantiatedTypesDependee = eps
    }

    def ownInstantiatedTypes: UIDSet[ReferenceType] = {
        if (_ownInstantiatedTypesDependee.hasUBP)
            _ownInstantiatedTypesDependee.ub.types
        else
            UIDSet.empty
    }

    def newInstantiatedTypes(seenTypes: Int): IterableOnce[ReferenceType] = {
        if (_ownInstantiatedTypesDependee.hasUBP) {
            _ownInstantiatedTypesDependee.ub.dropOldest(seenTypes)
        } else {
            UIDSet.empty
        }
    }

    /////////////////////////////////////////////
    //                                         //
    //      general dependency management      //
    //                                         //
    /////////////////////////////////////////////

    override def hasOpenDependencies: Boolean = {
        super.hasOpenDependencies || _ownInstantiatedTypesDependee.isRefinable
    }

    override def dependees: Set[SomeEOptionP] = {
        var dependees = super.dependees

        dependees += _ownInstantiatedTypesDependee

        dependees
    }

}
