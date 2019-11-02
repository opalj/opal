/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.ClassHierarchy
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEOptionP
import org.opalj.tac.fpcf.properties.TACAI

import scala.collection.mutable

/**
 * Manages the state of each method analyzed by [[TypePropagationAnalysis]].
 *
 * @param method The method under analysis.
 * @param setEntity The entity which holds the type set of the method.
 * @param _tacDependee Dependee for the three-address code of the method.
 * @param _ownInstantiatedTypesDependee Dependee for the type set of the method.
 * @param _calleeDependee Dependee for the callee property of the method.
 */
final class TypePropagationState(
        override val method:                       DefinedMethod,
        val setEntity:                             SetEntity,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI],

        private[this] var _ownInstantiatedTypesDependee: EOptionP[SetEntity, InstantiatedTypes],
        private[this] var _calleeDependee:               EOptionP[DefinedMethod, Callees]
) extends TACAIBasedAnalysisState {

    var methodWritesArrays: Boolean = false
    var methodReadsArrays: Boolean = false

    /////////////////////////////////////////////
    //                                         //
    //           own types (method)            //
    //                                         //
    /////////////////////////////////////////////

    def updateOwnInstantiatedTypesDependee(eps: EOptionP[SetEntity, InstantiatedTypes]): Unit = {
        _ownInstantiatedTypesDependee = eps
    }

    def ownInstantiatedTypes: UIDSet[ReferenceType] = {
        if (_ownInstantiatedTypesDependee.hasUBP)
            _ownInstantiatedTypesDependee.ub.types
        else
            UIDSet.empty
    }

    def newInstantiatedTypes(seenTypes: Int): TraversableOnce[ReferenceType] = {
        if (_ownInstantiatedTypesDependee.hasUBP) {
            _ownInstantiatedTypesDependee.ub.dropOldest(seenTypes)
        } else {
            UIDSet.empty
        }
    }

    /////////////////////////////////////////////
    //                                         //
    //                 callees                 //
    //                                         //
    /////////////////////////////////////////////

    private[this] var _seenCallees: mutable.Set[(PC, DeclaredMethod)] = mutable.Set.empty

    def isSeenCallee(pc: PC, callee: DeclaredMethod): Boolean = _seenCallees.contains((pc, callee))

    def addSeenCallee(pc: PC, callee: DeclaredMethod): Unit = {
        assert(!isSeenCallee(pc, callee))
        _seenCallees.add((pc, callee))
    }

    def calleeDependee: Option[EOptionP[DefinedMethod, Callees]] = {
        if (_calleeDependee.isRefinable) {
            Some(_calleeDependee)
        } else {
            None
        }
    }

    def updateCalleeDependee(calleeDependee: EOptionP[DefinedMethod, Callees]): Unit = {
        _calleeDependee = calleeDependee
    }

    /////////////////////////////////////////////
    //                                         //
    //           forward propagation           //
    //                                         //
    /////////////////////////////////////////////

    private[this] var _forwardPropagationEntities: mutable.Set[SetEntity] = mutable.Set.empty
    private[this] var _forwardPropagationFilters: mutable.Map[SetEntity, UIDSet[ReferenceType]] = mutable.Map.empty

    def forwardPropagationEntities: Traversable[SetEntity] = _forwardPropagationEntities

    def forwardPropagationFilters(setEntity: SetEntity): UIDSet[ReferenceType] = _forwardPropagationFilters(setEntity)

    /**
     * Registers a new set entity to consider for forward propagation alongside a set of filters. If the
     * set entity was already registered, the new type filters are added to the existing ones.
     *
     * @param setEntity The set entity to register.
     * @param typeFilters Set of types to filter for forward propagation.
     * @return True if the set of filters has changed compared to the ones which were previously known, otherwise
     *         False.
     */
    def registerForwardPropagationEntity(
        setEntity:   SetEntity,
        typeFilters: UIDSet[ReferenceType]
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Boolean = {
        assert(typeFilters.nonEmpty)
        val alreadyExists = _forwardPropagationEntities.contains(setEntity)
        if (!alreadyExists) {
            val compactedFilters = classHierarchy.rootTypes(typeFilters)
            _forwardPropagationEntities += setEntity
            _forwardPropagationFilters += setEntity -> compactedFilters
            true
        } else {
            val existingTypeFilters = _forwardPropagationFilters(setEntity)
            val newFilters = classHierarchy.rootTypes(existingTypeFilters union typeFilters)
            _forwardPropagationFilters.update(setEntity, newFilters)
            newFilters != existingTypeFilters
        }
    }

    /////////////////////////////////////////////
    //                                         //
    //           backward propagation          //
    //                                         //
    /////////////////////////////////////////////

    private[this] var _backwardPropagationDependees: mutable.Map[SetEntity, EOptionP[SetEntity, InstantiatedTypes]] =
        mutable.Map.empty
    private[this] var _backwardPropagationFilters: mutable.Map[SetEntity, UIDSet[ReferenceType]] = mutable.Map.empty

    def backwardPropagationDependeeInstantiatedTypes(setEntity: SetEntity): UIDSet[ReferenceType] = {
        val dependee = _backwardPropagationDependees(setEntity)
        if (dependee.hasUBP)
            dependee.ub.types
        else
            UIDSet.empty
    }

    def backwardPropagationDependeeIsRegistered(setEntity: SetEntity): Boolean =
        _backwardPropagationDependees.contains(setEntity)

    def backwardPropagationFilters(setEntity: SetEntity): UIDSet[ReferenceType] =
        _backwardPropagationFilters(setEntity)

    def updateBackwardPropagationFilters(
        setEntity:   SetEntity,
        typeFilters: UIDSet[ReferenceType]
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): Boolean = {
        assert(typeFilters.nonEmpty)
        val alreadyExists = _backwardPropagationFilters.contains(setEntity)
        if (!alreadyExists) {
            val compactedFilters = classHierarchy.rootTypes(typeFilters)
            _backwardPropagationFilters += setEntity -> compactedFilters
            true
        } else {
            val existingTypeFilters = _backwardPropagationFilters(setEntity)
            val newFilters = classHierarchy.rootTypes(existingTypeFilters union typeFilters)
            _backwardPropagationFilters.update(setEntity, newFilters)
            newFilters != existingTypeFilters
        }
    }

    def updateBackwardPropagationDependee(eps: EOptionP[SetEntity, InstantiatedTypes]): Unit = {
        _backwardPropagationDependees.update(eps.e, eps)
    }

    def seenTypes(setEntity: SetEntity): Int = {
        val dependee = _backwardPropagationDependees(setEntity)
        if (dependee.hasUBP)
            dependee.ub.numElements
        else
            0
    }

    /////////////////////////////////////////////
    //                                         //
    //      general dependency management      //
    //                                         //
    /////////////////////////////////////////////

    override def hasOpenDependencies: Boolean = {
        super.hasOpenDependencies ||
            _ownInstantiatedTypesDependee.isRefinable ||
            _calleeDependee.isRefinable ||
            _backwardPropagationDependees.nonEmpty
    }

    override def dependees: List[SomeEOptionP] = {
        var dependees = super.dependees

        dependees ::= _ownInstantiatedTypesDependee

        if (calleeDependee.isDefined)
            dependees ::= calleeDependee.get

        // Note: The values are copied here. The "++" operator on List
        // forces immediate evaluation of the map values iterator.
        dependees ++= _backwardPropagationDependees.values

        dependees
    }
}
