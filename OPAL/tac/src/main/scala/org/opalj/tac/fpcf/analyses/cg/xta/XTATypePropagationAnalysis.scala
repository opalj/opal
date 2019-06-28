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
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.collection.immutable.RefArray
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomePartialResult
import org.opalj.tac.fpcf.properties.TACAI
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.log.OPALLogger

class XTATypePropagationAnalysis private[analyses] ( final val project: SomeProject) extends ReachableMethodAnalysis {

    private type State = XTATypePropagationState

    override def processMethod(definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(definedMethod, InstantiatedTypes.key)
        val calleesEOptP = propertyStore(definedMethod, Callees.key)

        // Dependees for data flow through fields.
        // TODO AB optimize this...
        val (readFields, writtenFields) = findAccessedFieldsInBytecode(definedMethod)
        val readFieldTypeEOptPs = mutable.Map(readFields.map(f ⇒ f → propertyStore(f, InstantiatedTypes.key)).toSeq: _*)

        // TODO AB this is the level of accuracy as described in the Tip+Palsberg paper
        // Maybe we can track more accurately which array types are actually written/read.
        // (See notes.) Would probably increase complexity by quite a bit though.
        val containsArrayStores = definedMethod.definedMethod.body.get.exists { case (_, instr) ⇒ instr == AASTORE }
        val containsArrayLoads = definedMethod.definedMethod.body.get.exists { case (_, instr) ⇒ instr == AALOAD }

        val state =
            new XTATypePropagationState(definedMethod, tacEP, instantiatedTypesEOptP, calleesEOptP,
                writtenFields, readFieldTypeEOptPs, containsArrayStores, containsArrayLoads)

        // === Initial results ===
        // TODO AB reduce code duplication
        // If we already know types at this point, we possibly already have some results here (type flows to written
        // array types and written fields). Also, if the fields or arrays we read already had a non-empty type set,
        // we have initial backward flows.
        val initialResults = new ListBuffer[SomePartialResult]()

        if (calleesEOptP.hasUBP) {
            val callees = getCalleeDefinedMethods(calleesEOptP.ub)
            state.updateSeenCallees(callees)
            for (callee ← callees) {
                val methodFlowResults = handleNewCallee(state)(callee)
                initialResults ++= methodFlowResults
            }
        }

        for (f ← readFields; fieldEOptP = readFieldTypeEOptPs(f) if fieldEOptP.hasUBP) {
            val fieldTypes = fieldEOptP.ub.types

            initialResults += typeFlowPartialResult(state.method, fieldTypes)
            state.updateReadFieldSeenTypes(f, fieldEOptP.ub.numElements)
        }

        if (instantiatedTypesEOptP.hasUBP) {
            val initialTypes = instantiatedTypesEOptP.ub.types
            if (writtenFields.nonEmpty) {
                for (f ← writtenFields) {
                    val flowResult = forwardFlowToWrittenField(f, initialTypes)
                    if (flowResult.isDefined) {
                        initialResults += flowResult.get
                    }
                }
            }

            for (t ← initialTypes if t.isArrayType; at = t.asArrayType) {
                if (containsArrayStores) {
                    val flowResult = forwardFlowToArrayType(initialTypes)(at)
                    if (flowResult.isDefined) {
                        initialResults += flowResult.get
                    }
                }

                if (containsArrayLoads) {
                    val arrayTypeSetEOptP = propertyStore(at, InstantiatedTypes.key)
                    state.updateReadArrayInstantiatedTypesDependee(arrayTypeSetEOptP)
                    if (arrayTypeSetEOptP.hasUBP) {
                        val backwardFlowResult = typeFlowPartialResult(state.method, arrayTypeSetEOptP.ub.types)
                        initialResults += backwardFlowResult
                        state.updateArrayTypeSeenTypes(arrayTypeSetEOptP.e, arrayTypeSetEOptP.ub.numElements)
                    }
                }
            }
        }

        returnResults(initialResults)(state)
    }

    private def c(state: State)(eps: SomeEPS): ProperPropertyComputationResult = eps match {

        case EUBP(e: DefinedMethod, _: Callees) ⇒
            handleUpdateOfCallees(state, eps.asInstanceOf[EPS[DefinedMethod, Callees]])

        case EUBP(e: DefinedMethod, _: InstantiatedTypes) if e == state.method ⇒
            handleUpdateOfOwnTypeSet(state, eps.asInstanceOf[EPS[DefinedMethod, InstantiatedTypes]])

        case EUBP(e: DefinedMethod, _: InstantiatedTypes) ⇒
            handleUpdateOfCalleeTypeSet(state, eps.asInstanceOf[EPS[DefinedMethod, InstantiatedTypes]])

        case EUBP(e: Field, _: InstantiatedTypes) ⇒
            handleUpdateOfReadFieldTypeSet(state, eps.asInstanceOf[EPS[Field, InstantiatedTypes]])

        case EUBP(e: ArrayType, _: InstantiatedTypes) ⇒
            handleUpdateOfReadArrayTypeSet(state, eps.asInstanceOf[EPS[ArrayType, InstantiatedTypes]])

        case _ ⇒
            sys.error("received unexpected update")
    }

    private def handleUpdateOfCallees(state: State, eps: EPS[DefinedMethod, Callees]): ProperPropertyComputationResult = {

        state.updateCalleeDependee(eps)

        val allCallees = getCalleeDefinedMethods(eps.ub)

        val alreadySeenCallees = state.seenCallees
        val newCallees = allCallees diff alreadySeenCallees
        state.updateSeenCallees(allCallees)

        val newTypeResults = newCallees.flatMap(handleNewCallee(state))

        returnResults(newTypeResults)(state)
    }

    def handleNewCallee(state: State)(newCallee: DefinedMethod): Iterable[PartialResult[DefinedMethod, InstantiatedTypes]] = {
        // If the new callee is the method itself, that means it is recursive. We ignore these cases
        // since there is no relevant type flow.
        if (newCallee == state.method) {
            return Seq.empty;
        }

        val calleeInstantiatedTypesDependee = propertyStore(newCallee, InstantiatedTypes.key)
        state.updateCalleeInstantiatedTypesDependee(calleeInstantiatedTypesDependee)

        if (calleeInstantiatedTypesDependee.hasUBP) {
            val forwardResult = forwardFlow(state.method, newCallee, state.ownInstantiatedTypesUB)
            val backwardResult = backwardFlow(state.method, newCallee, calleeInstantiatedTypesDependee.ub.types)

            state.updateCalleeSeenTypes(newCallee, calleeInstantiatedTypesDependee.ub.numElements)

            forwardResult ++ backwardResult
        } else {
            state.updateCalleeSeenTypes(newCallee, 0)

            Seq.empty
        }
    }

    /**
     * As soon as the own type set updates, we can immediately re-check the known virtual call-sites within the
     * method for new targets.
     *
     * @param state State of the analysis for the current method.
     * @param eps Update of the own type set.
     * @return FPCF results.
     */
    def handleUpdateOfOwnTypeSet(state: State, eps: EPS[DefinedMethod, InstantiatedTypes]): ProperPropertyComputationResult = {

        val seenTypes = state.ownInstantiatedTypesUB.size
        state.updateOwnInstantiatedTypesDependee(eps)

        // TODO AB can this be empty?
        val newTypes = UIDSet(state.newInstantiatedTypes(seenTypes).toSeq: _*)

        // buffer which holds all partial results which are generated from this update
        val partialResults = mutable.ArrayBuffer[PartialResult[_ >: Null <: Entity, _ >: Null <: Property]]()

        // (1.) new types may flow to previously known callees, via parameters
        val forwardFlowResults = state.seenCallees.flatMap(c ⇒ forwardFlow(state.method, c, newTypes))
        partialResults ++= forwardFlowResults

        // (2.) new types may also flow to fields which are written in this method
        val flowToWrittenFields = state.writtenFields.flatMap(f ⇒ forwardFlowToWrittenField(f, newTypes))
        partialResults ++= flowToWrittenFields

        // (3.) new types may contain ArrayTypes, which need some special treatment
        val newArrayTypes = newTypes collect { case at: ArrayType ⇒ at }
        assert(newArrayTypes.forall(at ⇒ at.elementType.isReferenceType))

        if (newArrayTypes.nonEmpty) {
            // (3a.) if this method reads from arrays, we assume that all types previously written to the array
            // flow backward to this method
            if (state.methodReadsArrays) {
                partialResults ++= newArrayTypes.flatMap(backwardFlowFromNewArrayType(state))
            }
            // (3b.) if this method writes to arrays, compatible types from the type set of this method flow
            // to the type set of the new array type
            if (state.methodWritesArrays) {
                partialResults ++= newArrayTypes.flatMap(forwardFlowToArrayType(state.ownInstantiatedTypesUB))
            }
        }

        // (4.) if this method writes to arrays, all new types may flow to array types we already know via array stores
        if (state.methodWritesArrays) {
            val ownArrayTypes = state.ownInstantiatedTypesUB collect { case at: ArrayType ⇒ at }
            partialResults ++= ownArrayTypes.flatMap(forwardFlowToArrayType(newTypes))
        }

        returnResults(partialResults)(state)
    }

    def handleUpdateOfCalleeTypeSet(state: State, eps: EPS[DefinedMethod, InstantiatedTypes]): ProperPropertyComputationResult = {

        // If a callee has type updates, maybe these new types flow back to this method (the caller).

        val updatedCallee = eps.e
        val seenTypes = state.calleeSeenTypes(updatedCallee)

        state.updateCalleeInstantiatedTypesDependee(eps)

        val newCalleeTypes = eps.ub.dropOldest(seenTypes).toSeq

        state.updateCalleeSeenTypes(updatedCallee, seenTypes + newCalleeTypes.length)

        val backwardFlowResult = backwardFlow(state.method, updatedCallee, UIDSet(newCalleeTypes: _*))

        if (backwardFlowResult.isDefined) {
            Results(
                InterimPartialResult(state.dependees, c(state)),
                backwardFlowResult.get
            )
        } else {
            InterimPartialResult(state.dependees, c(state))
        }
    }

    def handleUpdateOfReadFieldTypeSet(state: State, eps: EPS[Field, InstantiatedTypes]): ProperPropertyComputationResult = {
        val updatedField = eps.e

        val seenTypes = state.fieldSeenTypes(updatedField)

        state.updateAccessedFieldInstantiatedTypesDependee(eps)

        val newReadFieldTypes = eps.ub.dropOldest(seenTypes).toSeq

        state.updateReadFieldSeenTypes(updatedField, seenTypes + newReadFieldTypes.length)

        val partialResult = typeFlowPartialResult(state.method, UIDSet(newReadFieldTypes: _*))

        Results(
            InterimPartialResult(state.dependees, c(state)),
            partialResult
        )
    }

    // TODO AB there is some duplication here (very similar to the field update method)
    def handleUpdateOfReadArrayTypeSet(state: State, eps: EPS[ArrayType, InstantiatedTypes]): ProperPropertyComputationResult = {
        assert(state.methodReadsArrays)

        val updatedArray = eps.e

        val seenTypes = state.arrayTypeSeenTypes(updatedArray)

        state.updateReadArrayInstantiatedTypesDependee(eps)

        val newReadArrayTypes = eps.ub.dropOldest(seenTypes).toSeq

        state.updateArrayTypeSeenTypes(updatedArray, seenTypes + newReadArrayTypes.length)

        val partialResult = typeFlowPartialResult(state.method, UIDSet(newReadArrayTypes: _*))

        Results(
            InterimPartialResult(state.dependees, c(state)),
            partialResult
        )
    }

    // calculate flow of new types from caller to callee
    private def forwardFlow(callerMethod: DefinedMethod, calleeMethod: DefinedMethod, newCallerTypes: UIDSet[ReferenceType]): Option[PartialResult[DefinedMethod, InstantiatedTypes]] = {

        // Note: Not only virtual methods, since the this pointer can also flow through private methods
        // which are called via invokespecial and are thus not considered "virtual" methods.
        val hasImplicitThisParameter = calleeMethod.definedMethod.isNotStatic

        val allParameterTypes: RefArray[FieldType] =
            if (hasImplicitThisParameter) {
                val implicitThisParamTypeUB = calleeMethod.declaringClassType.asFieldType
                implicitThisParamTypeUB +: calleeMethod.descriptor.parameterTypes
            } else {
                calleeMethod.descriptor.parameterTypes
            }

        // TODO AB performance..., maybe we can cache this stuff somehow
        val relevantParameterTypes = allParameterTypes.toSeq.filter(_.isReferenceType).map(_.asReferenceType)

        val newTypes = newCallerTypes.filter(t ⇒
            relevantParameterTypes.exists(p ⇒ classHierarchy.isSubtypeOf(t, p)))

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(calleeMethod, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    // calculate flow of new types from callee to caller
    private def backwardFlow(callerMethod: DefinedMethod, calleeMethod: DefinedMethod, newCalleeTypes: UIDSet[ReferenceType]): Option[PartialResult[DefinedMethod, InstantiatedTypes]] = {

        val returnTypeOfCallee = calleeMethod.descriptor.returnType

        // TODO AB optimization: don't register callee for possible backward flow in the first place
        if (!returnTypeOfCallee.isReferenceType) {
            return None;
        }

        val newTypes = newCalleeTypes.filter(classHierarchy.isSubtypeOf(_, returnTypeOfCallee.asReferenceType))

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(callerMethod, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    def forwardFlowToWrittenField(writtenField: Field, newTypesInMethod: UIDSet[ReferenceType]): Option[PartialResult[Field, InstantiatedTypes]] = {
        val fieldType = writtenField.fieldType

        // TODO AB this can be optimized since we never need to handle these fields
        if (!fieldType.isReferenceType) {
            return None;
        }

        val newTypes = newTypesInMethod.filter(t ⇒ classHierarchy.isSubtypeOf(t, fieldType.asReferenceType))

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(writtenField, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    def forwardFlowToArrayType(newTypes: UIDSet[ReferenceType])(arrayType: ArrayType): Option[PartialResult[ArrayType, InstantiatedTypes]] = {
        val forwardFlowingTypes = newTypes.filter(rt ⇒ classHierarchy.isSubtypeOf(rt, arrayType.elementType.asReferenceType))
        if (forwardFlowingTypes.nonEmpty)
            Some(typeFlowPartialResult(arrayType, forwardFlowingTypes))
        else
            None
    }

    // If we receive a new array type and the method reads from arrays, include the types written
    // to the array in the method's type set.
    def backwardFlowFromNewArrayType(state: State)(arrayType: ArrayType): Option[PartialResult[DefinedMethod, InstantiatedTypes]] = {
        val newTypeDependee = propertyStore(arrayType, InstantiatedTypes.key)
        state.updateReadArrayInstantiatedTypesDependee(newTypeDependee)

        assert(state.methodReadsArrays)

        if (newTypeDependee.hasUBP) {
            // We do not have to do any type checks here; if the array is read, the types stored in the array
            // (== subtypes of the array's element type) always flow to the reading method.
            val backwardFlow = typeFlowPartialResult(state.method, newTypeDependee.ub.types)
            state.updateArrayTypeSeenTypes(arrayType, newTypeDependee.ub.numElements)
            Some(backwardFlow)
        } else {
            state.updateArrayTypeSeenTypes(arrayType, 0)
            None
        }
    }

    def typeFlowPartialResult[E >: Null <: Entity](entity: E, newTypes: UIDSet[ReferenceType]): PartialResult[E, InstantiatedTypes] = {
        PartialResult[E, InstantiatedTypes](
            entity,
            InstantiatedTypes.key,
            updateInstantiatedTypes(entity, newTypes)
        )
    }

    // TODO AB something like this appears in several places; should maybe move to some Utils class
    // for now: mostly copied from InstantiatedTypesAnalysis; now generic so it works for methods and fields
    def updateInstantiatedTypes[E >: Null <: Entity](
        entity:               E,
        newInstantiatedTypes: UIDSet[ReferenceType]
    )(
        eop: EOptionP[E, InstantiatedTypes]
    ): Option[EPS[E, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) ⇒
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(entity, newUB))
            else
                None

        case _: EPK[_, _] ⇒
            val newUB = InstantiatedTypes.apply(newInstantiatedTypes)
            Some(InterimEUBP(entity, newUB))

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }

    def getCalleeDefinedMethods(callees: Callees): Set[DefinedMethod] = {
        // TODO AB check efficiency...
        val calleeMethods = mutable.Set[DefinedMethod]()
        for {
            pc ← callees.callSitePCs
            callee ← callees.callees(pc)
            if callee.hasSingleDefinedMethod // TODO AB should work on all DeclaredMethods?
        } {
            calleeMethods += callee.asDefinedMethod
        }
        calleeMethods.toSet
    }

    // TODO AB mostly a placeholder; likely it's better to get the accesses from TAC instead
    def findAccessedFieldsInBytecode(method: DefinedMethod): (Set[Field], Set[Field]) = {
        val code = method.definedMethod.body.get
        val reads = code.instructions.flatMap {
            case FieldReadAccess(objType, name, fieldType) ⇒
                val field = project.resolveFieldReference(objType, name, fieldType)
                if (field.isEmpty)
                    OPALLogger.warn("xta", s"field $name in $objType can not be resolved")
                field
            case _ ⇒
                None
        }.toSet
        val writes = code.instructions.flatMap {
            case FieldWriteAccess(objType, name, fieldType) ⇒
                val field = project.resolveFieldReference(objType, name, fieldType)
                if (field.isEmpty)
                    OPALLogger.warn("xta", s"field $name in $objType can not be resolved")
                field
            case _ ⇒
                None
        }.toSet

        (reads, writes)
    }

    private def returnResults(
        partialResults: TraversableOnce[SomePartialResult]
    )(implicit state: State): ProperPropertyComputationResult = {
        // Always re-register the continuation. It is impossible for all dependees to be final in XTA.
        Results(
            InterimPartialResult(state.dependees, c(state)),
            partialResults
        )
    }
}

object XTATypePropagationAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {
    override type InitializationData = Null

    // TODO AB handle assignment of initial instantiated types here!

    override def triggeredBy: PropertyKind = Callers.key

    override def register(project: SomeProject, propertyStore: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis = new XTATypePropagationAnalysis(project)
        propertyStore.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes, Callees, TACAI)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)
}