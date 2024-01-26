/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import java.util.HashMap as JHashMap
import java.util.HashSet as JHashSet
import java.util.Set as JSet
import scala.collection.immutable.IntMap
import scala.jdk.CollectionConverters.*

import org.opalj.br.ClassHierarchy
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldWriteAccessInformation
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEOptionP
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Manages the state of each method analyzed by [[TypePropagationAnalysis]].
 *
 * @param callContext The current call context of the method under analysis.
 * @param typeSetEntity The entity which holds the type set of the method.
 * @param _tacDependee Dependee for the three-address code of the method.
 * @param _ownInstantiatedTypesDependee Dependee for the type set of the method.
 * @param _calleeDependee Dependee for the callee property of the method.
 */
final class TypePropagationState[ContextType <: Context](
    override val callContext:                        ContextType,
    val typeSetEntity:                               TypeSetEntity,
    override protected[this] var _tacDependee:       EOptionP[Method, TACAI],
    private[this] var _ownInstantiatedTypesDependee: EOptionP[TypeSetEntity, InstantiatedTypes],
    private[this] var _calleeDependee:               EOptionP[DeclaredMethod, Callees],
    private[this] var _readAccessDependee:           EOptionP[Method, MethodFieldReadAccessInformation],
    private[this] var _writeAccessDependee:          EOptionP[Method, MethodFieldWriteAccessInformation]
) extends BaseAnalysisState with TACAIBasedAnalysisState[ContextType] {

    var methodWritesArrays: Boolean = false
    var methodReadsArrays: Boolean = false

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
    //                 callees                 //
    //                                         //
    /////////////////////////////////////////////

    private[this] val _seenCallees: JSet[(PC, DeclaredMethod)] = new JHashSet()

    def isSeenCallee(pc: PC, callee: DeclaredMethod): Boolean = _seenCallees.contains((pc, callee))

    def addSeenCallee(pc: PC, callee: DeclaredMethod): Unit = {
        assert(!isSeenCallee(pc, callee))
        _seenCallees.add((pc, callee))
    }

    def calleeDependee: Option[EOptionP[DeclaredMethod, Callees]] = {
        if (_calleeDependee.isRefinable) {
            Some(_calleeDependee)
        } else {
            None
        }
    }

    def updateCalleeDependee(calleeDependee: EOptionP[DeclaredMethod, Callees]): Unit = {
        _calleeDependee = calleeDependee
    }

    /////////////////////////////////////////////
    //                                         //
    //             field accesses              //
    //                                         //
    /////////////////////////////////////////////

    var seenDirectReadAccesses: IntMap[Int] = IntMap.empty
    var seenIndirectReadAccesses: IntMap[Int] = IntMap.empty
    var seenDirectWriteAccesses: IntMap[Int] = IntMap.empty
    var seenIndirectWriteAccesses: IntMap[Int] = IntMap.empty

    def readAccessDependee: Option[EOptionP[Method, MethodFieldReadAccessInformation]] = {
        if (_readAccessDependee.isRefinable) Some(_readAccessDependee) else None
    }

    def updateReadAccessDependee(readAccessDependee: EOptionP[Method, MethodFieldReadAccessInformation]): Unit = {
        _readAccessDependee = readAccessDependee
    }

    def writeAccessDependee: Option[EOptionP[Method, MethodFieldWriteAccessInformation]] = {
        if (_writeAccessDependee.isRefinable) Some(_writeAccessDependee) else None
    }

    def updateWriteAccessDependee(writeAccessDependee: EOptionP[Method, MethodFieldWriteAccessInformation]): Unit = {
        _writeAccessDependee = writeAccessDependee
    }

    /////////////////////////////////////////////
    //                                         //
    //           forward propagation           //
    //                                         //
    /////////////////////////////////////////////

    private[this] val _forwardPropagationEntities: JSet[TypeSetEntity] = new JHashSet()
    private[this] val _forwardPropagationFilters: JHashMap[TypeSetEntity, UIDSet[ReferenceType]] =
        new JHashMap()

    def forwardPropagationEntities: JSet[TypeSetEntity] = _forwardPropagationEntities

    def forwardPropagationFilters(typeSetEntity: TypeSetEntity): UIDSet[ReferenceType] =
        _forwardPropagationFilters.get(typeSetEntity)

    /**
     * Registers a new set entity to consider for forward propagation alongside a set of filters. If the
     * set entity was already registered, the new type filters are added to the existing ones.
     *
     * @param typeSetEntity The set entity to register.
     * @param typeFilters Set of types to filter for forward propagation.
     * @return True if the set of filters has changed compared to the ones which were previously known, otherwise
     *         False.
     */
    def registerForwardPropagationEntity(
        typeSetEntity: TypeSetEntity,
        typeFilters:   UIDSet[ReferenceType]
    )(
        implicit classHierarchy: ClassHierarchy
    ): Boolean = {
        assert(typeFilters.nonEmpty)
        val alreadyExists = _forwardPropagationEntities.contains(typeSetEntity)
        if (!alreadyExists) {
            val compactedFilters = rootTypes(typeFilters)
            _forwardPropagationEntities.add(typeSetEntity)
            _forwardPropagationFilters.put(typeSetEntity, compactedFilters)
            true
        } else {
            val existingTypeFilters = _forwardPropagationFilters.get(typeSetEntity)
            val newFilters = rootTypes(existingTypeFilters ++ typeFilters)
            _forwardPropagationFilters.put(typeSetEntity, newFilters)
            newFilters != existingTypeFilters
        }
    }

    /////////////////////////////////////////////
    //                                         //
    //           backward propagation          //
    //                                         //
    /////////////////////////////////////////////

    private[this] val _backwardPropagationDependees: JHashMap[TypeSetEntity, EOptionP[TypeSetEntity, InstantiatedTypes]] =
        new JHashMap()
    private[this] val _backwardPropagationFilters: JHashMap[TypeSetEntity, UIDSet[ReferenceType]] =
        new JHashMap()

    def backwardPropagationDependeeInstantiatedTypes(typeSetEntity: TypeSetEntity): UIDSet[ReferenceType] = {
        val dependee = _backwardPropagationDependees.get(typeSetEntity)
        if (dependee.hasUBP)
            dependee.ub.types
        else
            UIDSet.empty
    }

    def backwardPropagationDependeeIsRegistered(typeSetEntity: TypeSetEntity): Boolean =
        _backwardPropagationDependees.containsKey(typeSetEntity)

    def backwardPropagationFilters(typeSetEntity: TypeSetEntity): UIDSet[ReferenceType] =
        _backwardPropagationFilters.get(typeSetEntity)

    def updateBackwardPropagationFilters(
        typeSetEntity: TypeSetEntity,
        typeFilters:   UIDSet[ReferenceType]
    )(
        implicit classHierarchy: ClassHierarchy
    ): Boolean = {
        assert(typeFilters.nonEmpty)
        val alreadyExists = _backwardPropagationFilters.containsKey(typeSetEntity)
        if (!alreadyExists) {
            val compactedFilters = rootTypes(typeFilters)
            _backwardPropagationFilters.put(typeSetEntity, compactedFilters)
            true
        } else {
            val existingTypeFilters = _backwardPropagationFilters.get(typeSetEntity)
            val newFilters = rootTypes(existingTypeFilters ++ typeFilters)
            _backwardPropagationFilters.put(typeSetEntity, newFilters)
            newFilters != existingTypeFilters
        }
    }

    def updateBackwardPropagationDependee(eps: EOptionP[TypeSetEntity, InstantiatedTypes]): Unit = {
        _backwardPropagationDependees.put(eps.e, eps)
    }

    def seenTypes(typeSetEntity: TypeSetEntity): Int = {
        val dependee = _backwardPropagationDependees.get(typeSetEntity)
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
        !_backwardPropagationDependees.isEmpty
    }

    override def dependees: Set[SomeEOptionP] = {
        var dependees = super.dependees

        dependees += _ownInstantiatedTypesDependee

        if (calleeDependee.isDefined)
            dependees += calleeDependee.get
        if (readAccessDependee.isDefined)
            dependees += readAccessDependee.get
        if (writeAccessDependee.isDefined)
            dependees += writeAccessDependee.get

        // Note: The values are copied here. The "++" operator on List
        // forces immediate evaluation of the map values iterator.
        dependees ++= _backwardPropagationDependees.values().iterator.asScala

        dependees
    }

    /////////////////////////////////////////////
    //                                         //
    //      general helper functions           //
    //                                         //
    /////////////////////////////////////////////

    /**
     * For a given set of reference types, returns all types in the set for which no other type in
     * the set is a supertype.
     *
     * For example: If the set contains types A and B where B is a subtype of A, a single-element
     * set of A is returned.
     *
     * If the type java.lang.Object is in the input set, a single-element set containing just
     * java.lang.Object is returned, since Object is a supertype of all other types.
     */
    // IMPROVE: could be implemented with linear runtime.
    private[this] def rootTypes(
        types: UIDSet[ReferenceType]
    )(
        implicit classHierarchy: ClassHierarchy
    ): UIDSet[ReferenceType] = {
        if (types.size <= 1)
            return types;

        types.filter(t1 => !types.exists(t2 => t1 != t2 && classHierarchy.isSubtypeOf(t1, t2)))
    }
}
