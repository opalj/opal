/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.ArrayType
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.SomeEOptionP
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

        private[this] var _ownInstantiatedTypesDependee: EOptionP[DefinedMethod, InstantiatedTypes],

        private[this] var _calleeDependee: EOptionP[DefinedMethod, Callees],

        // Field stuff
        // TODO AB optimize...
        private[this] var _readFields:    Set[Field],
        private[this] var _writtenFields: Set[Field],
        // we only need type updates of fields the method reads
        private[this] var _readFieldTypeDependees: mutable.Map[Field, EOptionP[Field, InstantiatedTypes]],

        // Array stuff
        val methodWritesArrays: Boolean,
        val methodReadsArrays:  Boolean
) extends CGState {

    // TODO AB is there a more efficient data type for this?
    // TODO AB maybe we also need to store the PC of the callsite here, in order to optimize for returned values which are ignored
    private[this] var _seenCallees: mutable.Set[DefinedMethod] = mutable.Set.empty

    // TODO AB is there a better data type for this?
    private[this] var _calleeSeenTypes: mutable.LongMap[Int] = mutable.LongMap.empty

    // TODO AB Can these dependees become final? Probably not!
    // TODO AB Does this have to be a map?
    private[this] var _calleeInstantiatedTypesDependees: mutable.Map[DefinedMethod, EOptionP[DefinedMethod, InstantiatedTypes]] = mutable.Map.empty

    // NOTE AB functionally the same as calleeSeenTypes, but Field does not have an ID we can use
    private[this] var _readFieldSeenTypes: mutable.Map[Field, Int] = mutable.Map.empty

    // Note: ArrayType uses a cache internally, so identical array types will be represented by the same object.
    private[this] var _readArraysTypeDependees: mutable.Map[ArrayType, EOptionP[ArrayType, InstantiatedTypes]] = mutable.Map.empty
    private[this] var _readArraysSeenTypes: mutable.Map[ArrayType, Int] = mutable.Map.empty

    private[this] val _virtualCallSites: mutable.LongMap[mutable.Set[CallSiteT]] = mutable.LongMap.empty

    /////////////////////////////////////////////
    //                                         //
    //          instantiated types             //
    //                                         //
    /////////////////////////////////////////////

    def updateOwnInstantiatedTypesDependee(
        ownInstantiatedTypesDependee: EOptionP[DefinedMethod, InstantiatedTypes]
    ): Unit = {
        _ownInstantiatedTypesDependee = ownInstantiatedTypesDependee
    }

    def ownInstantiatedTypesDependee: Option[EOptionP[DefinedMethod, InstantiatedTypes]] = {
        if (_ownInstantiatedTypesDependee.isRefinable)
            Some(_ownInstantiatedTypesDependee)
        else
            None
    }

    def ownInstantiatedTypesUB: UIDSet[ReferenceType] = {
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
    //                callees                  //
    //                                         //
    /////////////////////////////////////////////

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

    def seenCallees: Set[DefinedMethod] = {
        // TODO AB probably not very efficient
        _seenCallees.toSet
    }

    def updateSeenCallees(newCallees: Set[DefinedMethod]): Unit = {
        _seenCallees ++= newCallees
    }

    def calleeSeenTypes(callee: DefinedMethod): Int = {
        _calleeSeenTypes(callee.id.toLong)
    }

    def updateCalleeSeenTypes(callee: DefinedMethod, numberOfTypes: Int): Unit = {
        assert(numberOfTypes >= _calleeSeenTypes.getOrElse(callee.id.toLong, 0))
        _calleeSeenTypes.update(callee.id.toLong, numberOfTypes)
    }

    def updateCalleeInstantiatedTypesDependee(
        eps: EOptionP[DefinedMethod, InstantiatedTypes]
    ): Unit = {
        _calleeInstantiatedTypesDependees.update(eps.e, eps)
    }

    /////////////////////////////////////////////
    //                                         //
    //                 fields                  //
    //                                         //
    /////////////////////////////////////////////

    def writtenFields: Set[Field] = _writtenFields

    def updateAccessedFieldInstantiatedTypesDependee(
        eps: EOptionP[Field, InstantiatedTypes]
    ): Unit = {
        _readFieldTypeDependees.update(eps.e, eps)
    }

    def fieldSeenTypes(field: Field): Int = {
        _readFieldSeenTypes.getOrElse(field, 0)
    }

    def updateReadFieldSeenTypes(field: Field, numberOfTypes: Int): Unit = {
        assert(numberOfTypes >= _readFieldSeenTypes.getOrElse(field, 0))
        _readFieldSeenTypes.update(field, numberOfTypes)
    }

    /////////////////////////////////////////////
    //                                         //
    //                 arrays                  //
    //                                         //
    /////////////////////////////////////////////

    def availableArrayTypes: UIDSet[ArrayType] = {
        ownInstantiatedTypesUB collect { case at: ArrayType ⇒ at }
    }

    def updateReadArrayInstantiatedTypesDependee(
        eps: EOptionP[ArrayType, InstantiatedTypes]
    ): Unit = {
        _readArraysTypeDependees.update(eps.e, eps)
    }

    def arrayTypeSeenTypes(arrayType: ArrayType): Int = {
        _readArraysSeenTypes.getOrElse(arrayType, 0)
    }

    def updateArrayTypeSeenTypes(arrayType: ArrayType, numberOfTypes: Int): Unit = {
        assert(numberOfTypes >= arrayTypeSeenTypes(arrayType))
        _readArraysSeenTypes.update(arrayType, numberOfTypes)
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
            _calleeDependee.isRefinable ||
            _calleeInstantiatedTypesDependees.nonEmpty ||
            _readFieldTypeDependees.nonEmpty ||
            _readArraysSeenTypes.nonEmpty
    }

    override def dependees: List[SomeEOptionP] = {
        var dependees = super.dependees

        if (ownInstantiatedTypesDependee.isDefined)
            dependees ::= ownInstantiatedTypesDependee.get

        if (calleeDependee.isDefined)
            dependees ::= calleeDependee.get

        if (_calleeInstantiatedTypesDependees.nonEmpty)
            dependees ++= _calleeInstantiatedTypesDependees.values

        if (_readFieldTypeDependees.nonEmpty)
            dependees ++= _readFieldTypeDependees.values

        if (_readArraysTypeDependees.nonEmpty)
            dependees ++= _readArraysTypeDependees.values

        dependees
    }
}
