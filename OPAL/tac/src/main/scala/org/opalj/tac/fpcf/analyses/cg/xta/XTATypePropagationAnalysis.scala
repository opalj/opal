/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.time.Instant

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.log.OPALLogger
import org.opalj.collection.immutable.RefArray
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomePartialResult
import org.opalj.br.ArrayType
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.DeclaredMethod
import org.opalj.br.MultipleDefinedMethods
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.tac.fpcf.properties.TACAI

// TODO AB helper for debugging and maybe evaluation later...
private[xta] class TypePropagationTrace {
    private val _file = s"C:\\Users\\Andreas\\Dropbox\\Masterarbeit\\traces\\trace${Instant.now.getEpochSecond}.txt"
    private val _out = new PrintWriter(new FileOutputStream(new File(_file)))

    private def trace(msg: String): Unit = {
        _out.println(msg)
        _out.flush()
    }

    private def simplifiedName(e: Any): String = e match {
        case defM: DefinedMethod ⇒ s"${simplifiedName(defM.declaringClassType)}.${defM.name}(...)"
        case rt: ReferenceType ⇒ rt.toJava.substring(rt.toJava.lastIndexOf('.') + 1)
        case _ ⇒ e.toString
    }

    def traceInit(method: DefinedMethod)(implicit ps: PropertyStore, dm: DeclaredMethods): Unit = {
        val initialTypes = {
            val typeEOptP = ps(method, InstantiatedTypes.key)
            if (typeEOptP.hasUBP) typeEOptP.ub.types
            else UIDSet.empty
        }
        val initialCallees =  {
            val calleesEOptP = ps(method, Callees.key)
            if (calleesEOptP.hasUBP) calleesEOptP.ub.callSites.flatMap(_._2)
            else Iterator.empty
        }
        trace(s"init: ${simplifiedName(method)} (initial types: {${initialTypes.map(simplifiedName).mkString(", ")}}, initial callees: {${initialCallees.map(simplifiedName).mkString(", ")}})")
    }

    def traceCalleesUpdate(method: DefinedMethod): Unit = {
        trace(s"callee property update: ${simplifiedName(method)}")
    }

    def traceNewCallee(method: DefinedMethod, newCallee: DeclaredMethod): Unit = {
        trace(s"new callee for ${simplifiedName(method)}: ${simplifiedName(newCallee)}")
    }

    def traceTypeUpdate(method: DefinedMethod, updatedEntity: Entity, types: UIDSet[ReferenceType]): Unit = {
        trace(s"type set update: for ${simplifiedName(method)}, from ${simplifiedName(updatedEntity)}, with types: {${types.map(simplifiedName).mkString(", ")}}")
    }

    def traceTypePropagation(entity: Entity, newTypes: UIDSet[ReferenceType]): Unit = {
        trace(s"propagate {${newTypes.map(simplifiedName).mkString(", ")}} to ${simplifiedName(entity)}")
    }
}

class XTATypePropagationAnalysis private[analyses] ( final val project: SomeProject) extends ReachableMethodAnalysis {
    private[this] val _trace: TypePropagationTrace = new TypePropagationTrace()

    private type State = XTATypePropagationState

    override def processMethod(definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(definedMethod, InstantiatedTypes.key)
        val calleesEOptP = propertyStore(definedMethod, Callees.key)

        _trace.traceInit(definedMethod)

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
            val callees = getCalleeDeclaredMethods(calleesEOptP.ub)
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
            assert(e == state.method)
            _trace.traceCalleesUpdate(e)
            handleUpdateOfCallees(state, eps.asInstanceOf[EPS[DefinedMethod, Callees]])

        case EUBP(e: DefinedMethod, t: InstantiatedTypes) if e == state.method ⇒
            _trace.traceTypeUpdate(state.method, e, t.types)
            handleUpdateOfOwnTypeSet(state, eps.asInstanceOf[EPS[DefinedMethod, InstantiatedTypes]])

        case EUBP(e: DefinedMethod, t: InstantiatedTypes) ⇒
            _trace.traceTypeUpdate(state.method, e, t.types)
            handleUpdateOfCalleeTypeSet(state, eps.asInstanceOf[EPS[DefinedMethod, InstantiatedTypes]])

        case EUBP(ExternalWorld, t: InstantiatedTypes) ⇒
            _trace.traceTypeUpdate(state.method, ExternalWorld, t.types)
            handleUpdateOfExternalWorldTypeSet(state, eps.asInstanceOf[EPS[Entity, InstantiatedTypes]])

        case EUBP(e: Field, t: InstantiatedTypes) ⇒
            _trace.traceTypeUpdate(state.method, e, t.types)
            handleUpdateOfReadFieldTypeSet(state, eps.asInstanceOf[EPS[Field, InstantiatedTypes]])

        case EUBP(e: ArrayType, t: InstantiatedTypes) ⇒
            _trace.traceTypeUpdate(state.method, e, t.types)
            handleUpdateOfReadArrayTypeSet(state, eps.asInstanceOf[EPS[ArrayType, InstantiatedTypes]])

        case _ ⇒
            sys.error("received unexpected update")
    }

    private def handleUpdateOfCallees(state: State, eps: EPS[DefinedMethod, Callees]): ProperPropertyComputationResult = {

        state.updateCalleeDependee(eps)

        val allCallees = getCalleeDeclaredMethods(eps.ub)

        val alreadySeenCallees = state.seenCallees
        val newCallees = allCallees diff alreadySeenCallees
        state.updateSeenCallees(allCallees)

        val newTypeResults = newCallees.flatMap(handleNewCallee(state))

        returnResults(newTypeResults)(state)
    }

    def handleNewCallee(state: State)(newCallee: DeclaredMethod): Iterable[SomePartialResult] = {
        _trace.traceNewCallee(state.method, newCallee)

        // If the new callee is the method itself, that means it is recursive. We ignore these cases
        // since there is no relevant type flow.
        if (newCallee == state.method) {
            return Seq.empty;
        }

        // TODO AB think about how to handle this case
        assert(!newCallee.hasMultipleDefinedMethods)

        // TODO AB for debugging; remove later
        if (newCallee.descriptor.parameterTypes.filter(_.isObjectType).exists(ot ⇒ !classHierarchy.isKnown(ot.asObjectType))) {
            OPALLogger.warn("xta", s"new callee $newCallee has parameter types not known by the class hierarchy; type flows could be incorrect")
        }

        val calleeInstantiatedTypesDependee =
            if (newCallee.hasSingleDefinedMethod) {
                val calleeDefinedMethodInstantiatedTypesDependee = propertyStore(newCallee.asDefinedMethod, InstantiatedTypes.key)
                state.updateCalleeInstantiatedTypesDependee(calleeDefinedMethodInstantiatedTypesDependee)
                calleeDefinedMethodInstantiatedTypesDependee
            } else {
                val externalWorldInstantiatedTypesDependee = propertyStore(ExternalWorld, InstantiatedTypes.key)
                state.updateExternalWorldInstantiatedTypesDependee(externalWorldInstantiatedTypesDependee)
                externalWorldInstantiatedTypesDependee
            }

        val forwardResult = forwardFlow(state.method, newCallee, state.ownInstantiatedTypesUB)

        if (calleeInstantiatedTypesDependee.hasUBP) {
            val backwardResult = backwardFlow(state.method, newCallee, calleeInstantiatedTypesDependee.ub.types)

            if (newCallee.hasSingleDefinedMethod) {
                state.updateCalleeSeenTypes(newCallee.asDefinedMethod, calleeInstantiatedTypesDependee.ub.numElements)
            } else {
                state.updateExternalWorldSeenTypes(calleeInstantiatedTypesDependee.ub.numElements)
            }

            forwardResult ++ backwardResult
        } else {
            if (newCallee.hasSingleDefinedMethod) {
                state.updateCalleeSeenTypes(newCallee.asDefinedMethod, 0)
            }

            forwardResult
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
        val partialResults = mutable.ArrayBuffer[SomePartialResult]()

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

    def handleUpdateOfExternalWorldTypeSet(state: State, eps: EPS[Entity, InstantiatedTypes]): ProperPropertyComputationResult = {

        // TODO AB Think about this case later ...
        assert(!state.seenCallees.exists(_.isInstanceOf[MultipleDefinedMethods]))

        val seenTypes = state.externalWorldSeenTypes

        state.updateExternalWorldInstantiatedTypesDependee(eps)

        val unseenTypes = eps.ub.dropOldest(seenTypes).toSeq

        // TODO AB caching/etc for performance optimization...
        // The update could have come through the return value of any external callee. We do not know which one.
        val relevantReturnTypes = state.seenCallees.collect {
            case c: VirtualDeclaredMethod if c.descriptor.returnType.isReferenceType ⇒ c.descriptor.returnType.asReferenceType
        }

        val newTypes = unseenTypes.filter(subType ⇒
            relevantReturnTypes.exists(superType ⇒ classHierarchy.isSubtypeOf(subType, superType)))

        state.updateExternalWorldSeenTypes(seenTypes + unseenTypes.length)

        val results =
            if (newTypes.isEmpty) Iterable.empty
            else Iterable(typeFlowPartialResult(state.method, UIDSet(newTypes: _*)))

        returnResults(results)(state)
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
    private def forwardFlow(callerMethod: DefinedMethod, calleeMethod: DeclaredMethod, newCallerTypes: UIDSet[ReferenceType]): Option[SomePartialResult] = {

        // Special case: Object.<init> is implicitly called as a super call by any method X.<init>.
        // The "this" type X will flow to the type set of Object.<init>. Since Object.<init> is usually
        // part of the external world, the external world type set is then polluted with any types which
        // was constructed in the program somewhere.
        // TODO AB Maybe this case can be handled more gracefully.
        if (calleeMethod.declaringClassType == ObjectType.Object && calleeMethod.name == "<init>") {
            return None;
        }

        // Note: Not only virtual methods, since the this pointer can also flow through private methods
        // which are called via invokespecial and are thus not considered "virtual" methods.

        // TODO AB this is no good
        // For now, assume all methods for which we do not have a defined method (--> external world) take an implicit "this"
        val hasImplicitThisParameter = !calleeMethod.hasSingleDefinedMethod || calleeMethod.definedMethod.isNotStatic

        val allParameterTypes: RefArray[FieldType] =
            if (hasImplicitThisParameter) {
                val implicitThisParamTypeUB = calleeMethod.declaringClassType.asFieldType
                implicitThisParamTypeUB +: calleeMethod.descriptor.parameterTypes
            } else {
                calleeMethod.descriptor.parameterTypes
            }

        // TODO AB performance..., maybe we can cache this stuff somehow
        val relevantParameterTypes = allParameterTypes.toSeq.collect { case rt: ReferenceType ⇒ rt }

        val newTypes = newCallerTypes.filter(subType ⇒
            relevantParameterTypes.exists(superType ⇒ classHierarchy.isSubtypeOf(subType, superType)))

        val flowTarget =
            if (calleeMethod.hasSingleDefinedMethod)
                calleeMethod.asDefinedMethod
            else if (calleeMethod.hasMultipleDefinedMethods)
                // TODO AB think about how to handle these cases
                sys.error(s"Case not implemented: MultipleDefinedMethods $calleeMethod")
            else
                ExternalWorld

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(flowTarget, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    // calculate flow of new types from callee to caller
    private def backwardFlow(callerMethod: DefinedMethod, calleeMethod: DeclaredMethod, newCalleeTypes: UIDSet[ReferenceType]): Option[SomePartialResult] = {

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
        _trace.traceTypePropagation(entity, newTypes)
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

    def getCalleeDeclaredMethods(callees: Callees): Set[DeclaredMethod] = {
        // TODO AB have to iterate through all methods for each update; can we make this more efficient?
        val calleeMethods = mutable.Set[DeclaredMethod]()
        for {
            pc ← callees.callSitePCs
            callee ← callees.callees(pc)
        } {
            calleeMethods += callee
        }
        // meh
        calleeMethods.toSet
    }

    // TODO AB mostly a placeholder; likely it's better to get the accesses from TAC instead
    def findAccessedFieldsInBytecode(method: DefinedMethod): (Set[Field], Set[Field]) = {
        val code = method.definedMethod.body.get
        val reads = code.instructions.flatMap {
            case FieldReadAccess(objType, name, fieldType) ⇒
                val field = project.resolveFieldReference(objType, name, fieldType)
                if (field.isEmpty)
                    OPALLogger.warn("xta", s"field $name in $objType can not be resolved (read)")
                field
            case _ ⇒
                None
        }.toSet
        val writes = code.instructions.flatMap {
            case FieldWriteAccess(objType, name, fieldType) ⇒
                val field = project.resolveFieldReference(objType, name, fieldType)
                if (field.isEmpty)
                    OPALLogger.warn("xta", s"field $name in $objType can not be resolved (write)")
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

    override def triggeredBy: PropertyKind = Callers.key

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        val declaredMethods = p.get(DeclaredMethodsKey)
        val entryPoints = p.get(InitialEntryPointsKey)
        // TODO AB placeholder!
        val initialInstantiatedTypes = UIDSet(ObjectType.String, ArrayType(ObjectType.String))

        // Pre-initialize [Ljava/lang/String;
        ps.preInitialize(ArrayType(ObjectType.String), InstantiatedTypes.key) {
            case _: EPK[_, _] ⇒ InterimEUBP(ArrayType(ObjectType.String), InstantiatedTypes(UIDSet(ObjectType.String)))
            case eps          ⇒ throw new IllegalStateException(s"unexpected property: $eps")
        }

        // TODO AB more sophisticated handling needed for library mode!
        for (ep ← entryPoints; method = declaredMethods(ep)) {
            if (method.name != "main") {
                OPALLogger.warn("xta", "initial type assignment to entry points other than 'main' methods not implemented yet!")(p.logContext)
            } else {
                ps.preInitialize(method, InstantiatedTypes.key) {
                    case _: EPK[_, _] ⇒ InterimEUBP(method, InstantiatedTypes(initialInstantiatedTypes))
                    case eps          ⇒ throw new IllegalStateException(s"unexpected property: $eps")
                }
            }
        }

        null
    }

    override def register(project: SomeProject, propertyStore: PropertyStore, i: Null): FPCFAnalysis = {
        val analysis = new XTATypePropagationAnalysis(project)
        propertyStore.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes, Callees, TACAI)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(InstantiatedTypes)
}