/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.collection.ForeachRefIterator
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
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.properties.TACAI
import scala.collection.mutable

import org.opalj.log.OPALLogger

/**
 * XTA is a dataflow-based call graph analysis which was introduced by Tip and Palsberg.
 *
 * This analysis does not handle features such as JVM calls to static initializers or finalize
 * calls.
 * However, analyses for these features (e.g. [[org.opalj.tac.fpcf.analyses.cg.FinalizerAnalysis]]
 * or the [[org.opalj.tac.fpcf.analyses.cg.LoadedClassesAnalysis]]) can be executed within the
 * same batch and the call graph will be generated in collaboration.
 *
 * @author Andreas Bauer
 */
class XTACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractCallGraphAnalysis {

    // TODO maybe cache results for Object.toString, Iterator.hasNext, Iterator.next

    private[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    override type State = XTAState

    override def c(state: XTAState)(eps: SomeEPS): ProperPropertyComputationResult = eps match {

        case EUBP(e: DefinedMethod, _: InstantiatedTypes) ⇒
            if (e == state.method) {
                handleUpdateOfOwnTypeSet(state, eps.asInstanceOf[EPS[DefinedMethod, InstantiatedTypes]])
            } else {
                handleUpdateOfCalleeTypeSet(state, eps.asInstanceOf[EPS[DefinedMethod, InstantiatedTypes]])
            }

        case EUBP(e: Field, _: InstantiatedTypes) ⇒
            handleUpdateOfReadFieldTypeSet(state, eps.asInstanceOf[EPS[Field, InstantiatedTypes]])

        case EUBP(e: DefinedMethod, callees: Callees) ⇒
            state.updateCalleeDependee(eps.asInstanceOf[EPS[DefinedMethod, Callees]])

            val allCallees = getCalleeDefinedMethods(callees)

            val alreadySeenCallees = state.seenCallees

            val newCallees = allCallees diff alreadySeenCallees

            // TODO AB improve this
            state.updateSeenCallees(allCallees)

            val newTypeResults = newCallees.flatMap(handleNewCallee(state))

            if (newTypeResults.nonEmpty) {
                Results(
                    InterimPartialResult(state.dependees, c(state)),
                    newTypeResults
                )
            } else {
                InterimPartialResult(state.dependees, c(state))
            }

        case _ ⇒ super.c(state)(eps)
    }

    def handleNewCallee(state: XTAState)(newCallee: DefinedMethod): Iterable[PartialResult[DefinedMethod, InstantiatedTypes]] = {
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
     * @return FPCF results..
     */
    def handleUpdateOfOwnTypeSet(state: XTAState, eps: EPS[DefinedMethod, InstantiatedTypes]): ProperPropertyComputationResult = {

        val seenTypes = state.ownInstantiatedTypesUB.size

        state.updateOwnInstantiatedTypesDependee(eps)

        // we only want to add the new calls, so we create a fresh object
        val calleesAndCallers = new DirectCalls()

        handleVirtualCallSites(calleesAndCallers, seenTypes)(state)

        // TODO AB we need to convert the iterator immediately, since we can only use the iterator once
        val newTypes = state.newInstantiatedTypes(seenTypes).toSeq
        val forwardFlowResults = state.seenCallees.flatMap(c ⇒ forwardFlow(state.method, c, UIDSet(newTypes: _*)))

        val flowToWrittenFields = state.writtenFields.flatMap(f ⇒ forwardFlowToWrittenField(f, UIDSet(newTypes: _*)))

        returnResult(calleesAndCallers, forwardFlowResults, flowToWrittenFields)(state)
    }

    // TODO AB (mostly) copied from super-class since we need to make some adjustments
    // this is the first returnResults called by the base class after TAC is available,
    // we need to override and modify this since XTA has other requirements
    override protected def returnResult(
        calleesAndCallers: DirectCalls
    )(implicit state: State): ProperPropertyComputationResult = {
        val results = calleesAndCallers.partialResults(state.method)

        // NOTE AB removed "state.hasNonFinalCallSite &&"
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(state)),
                results
            )
        else
            Results(results)
    }

    // TODO (mostly) copied from super-class
    protected def returnResult(
        calleesAndCallers:           DirectCalls,
        typeFlowPartialResults:      TraversableOnce[PartialResult[DefinedMethod, InstantiatedTypes]],
        fieldTypeFlowPartialResults: TraversableOnce[PartialResult[Field, InstantiatedTypes]]
    )(implicit state: State): ProperPropertyComputationResult = {
        val results = calleesAndCallers.partialResults(state.method)

        // TODO AB is it possible to have no open dependencies in XTA? i don't think so!
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(state)),
                // TODO AB not efficient?
                results.toIterator ++ typeFlowPartialResults ++ fieldTypeFlowPartialResults
            )
        else
            Results(results.toIterator ++ typeFlowPartialResults ++ fieldTypeFlowPartialResults)
    }

    def handleUpdateOfCalleeTypeSet(state: XTAState, eps: EPS[DefinedMethod, InstantiatedTypes]): ProperPropertyComputationResult = {

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

    // calculate flow of new types from caller to callee
    private def forwardFlow(callerMethod: DefinedMethod, calleeMethod: DefinedMethod, newCallerTypes: UIDSet[ObjectType]): Option[PartialResult[DefinedMethod, InstantiatedTypes]] = {

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

        // TODO AB handle other types than ObjectTypes (e.g. arrays, primitive types like int?)
        // TODO AB performance...; maybe cache this stuff somehow
        val relevantParameterTypes = allParameterTypes.toSeq.filter(_.isObjectType).map(_.asObjectType)

        val newTypes = newCallerTypes.filter(t ⇒ relevantParameterTypes.exists(p ⇒ t.isSubtypeOf(p)))

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(calleeMethod, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    def forwardFlowToWrittenField(writtenField: Field, newTypesInMethod: UIDSet[ObjectType]): Option[PartialResult[Field, InstantiatedTypes]] = {
        val fieldType = writtenField.fieldType

        // TODO AB handle special cases
        if (!fieldType.isObjectType) {
            return None;
        }

        val fieldObjType = fieldType.asObjectType

        val newTypes = newTypesInMethod.filter(t ⇒ t.isSubtypeOf(fieldObjType))

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(writtenField, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    // calculate flow of new types from callee to caller
    private def backwardFlow(callerMethod: DefinedMethod, calleeMethod: DefinedMethod, newCalleeTypes: UIDSet[ObjectType]): Option[PartialResult[DefinedMethod, InstantiatedTypes]] = {

        val returnTypeOfCallee = calleeMethod.descriptor.returnType
        if (!returnTypeOfCallee.isObjectType) {
            return None;
        }

        val newTypes = newCalleeTypes.filter(t ⇒ t.isSubtypeOf(returnTypeOfCallee.asObjectType))

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(callerMethod, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    def typeFlowPartialResult[E >: Null <: Entity](entity: E, newTypes: UIDSet[ObjectType]): PartialResult[E, InstantiatedTypes] = {
        PartialResult[E, InstantiatedTypes](
            entity,
            InstantiatedTypes.key,
            updateInstantiatedTypes(entity, newTypes)
        )
    }

    // for now: mostly copied from InstantiatedTypesAnalysis; now generic so it works for methods and fields
    def updateInstantiatedTypes[E >: Null <: Entity](
        entity:               E,
        newInstantiatedTypes: UIDSet[ObjectType]
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
            if callee.hasSingleDefinedMethod // TODO AB bad, should work on DeclaredMethods...
        } {
            calleeMethods += callee.asDefinedMethod
        }
        calleeMethods.toSet
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): XTAState = {
        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(definedMethod, InstantiatedTypes.key)
        val calleesEOptP = propertyStore(definedMethod, Callees.key)

        // Dependees for data flow through fields.
        // TODO AB optimize this...
        val (readFields, writtenFields) = findAccessedFieldsInBytecode(definedMethod)
        val readFieldTypeEOptPs = mutable.Map(readFields.map(f ⇒ f → propertyStore(f, InstantiatedTypes.key)).toSeq: _*)

        // If we already know types at this point and the method writes to fields, we possibly already
        // have some results here.
        if (instantiatedTypesEOptP.hasUBP && writtenFields.nonEmpty) {
            // TODO AB fix this bug, likely requires adjustments to the base class
            OPALLogger.warn("xta", "initial types already available, propagation to written field(s) not possible!")
        }

        new XTAState(definedMethod, tacEP, instantiatedTypesEOptP, calleesEOptP,
            readFields, writtenFields, readFieldTypeEOptPs)
    }

    // TODO AB mostly a placeholder; likely it's better to get the accesses from TAC instead
    def findAccessedFieldsInBytecode(method: DefinedMethod): (Set[Field], Set[Field]) = {
        val code = method.definedMethod.body.get
        val reads = code.instructions.flatMap {
            case FieldReadAccess(objType, name, fieldType) ⇒
                project.resolveFieldReference(objType, name, fieldType)
            case _ ⇒
                None
        }.toSet
        val writes = code.instructions.flatMap {
            case FieldWriteAccess(objType, name, fieldType) ⇒
                project.resolveFieldReference(objType, name, fieldType)
            case _ ⇒
                None
        }.toSet

        (reads, writes)
    }

    def handleUpdateOfReadFieldTypeSet(state: XTAState, eps: EPS[Field, InstantiatedTypes]): ProperPropertyComputationResult = {
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

    override def handleImpreciseCall(
        caller:                        DefinedMethod,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        potentialTargets:              ForeachRefIterator[ObjectType],
        calleesAndCallers:             DirectCalls
    )(implicit state: XTAState): Unit = {
        for (possibleTgtType ← potentialTargets) {
            if (state.ownInstantiatedTypesUB.contains(possibleTgtType)) {
                val tgtR = project.instanceCall(
                    caller.declaringClassType.asObjectType,
                    possibleTgtType,
                    call.name,
                    call.descriptor
                )

                handleCall(
                    caller,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    pc,
                    tgtR,
                    calleesAndCallers
                )
            } else {
                state.addVirtualCallSite(
                    possibleTgtType, (pc, call.name, call.descriptor, call.declaringClass)
                )
            }
        }

        // TODO: Document what happens here
        if (specializedDeclaringClassType.isObjectType) {
            val declType = specializedDeclaringClassType.asObjectType

            val mResult = if (classHierarchy.isInterface(declType).isYes)
                org.opalj.Result(project.resolveInterfaceMethodReference(
                    declType, call.name, call.descriptor
                ))
            else
                org.opalj.Result(project.resolveMethodReference(
                    declType,
                    call.name,
                    call.descriptor,
                    forceLookupInSuperinterfacesOnFailure = true
                ))

            if (mResult.isEmpty) {
                unknownLibraryCall(
                    caller,
                    call.name,
                    call.descriptor,
                    call.declaringClass,
                    declType,
                    caller.definedMethod.classFile.thisType.packageName,
                    pc,
                    calleesAndCallers
                )
            } else if (isMethodOverridable(mResult.value).isYesOrUnknown) {
                calleesAndCallers.addIncompleteCallSite(pc)
            }
        }
    }

    // modifies state and the calleesAndCallers
    private[this] def handleVirtualCallSites(
        calleesAndCallers: DirectCalls, seenTypes: Int
    )(implicit state: XTAState): Unit = {
        state.newInstantiatedTypes(seenTypes).foreach { instantiatedType ⇒
            val callSites = state.getVirtualCallSites(instantiatedType)
            callSites.foreach { callSite ⇒
                val (pc, name, descr, declaringClass) = callSite
                val tgtR = project.instanceCall(
                    state.method.definedMethod.classFile.thisType,
                    instantiatedType,
                    name,
                    descr
                )

                handleCall(
                    state.method,
                    name,
                    descr,
                    declaringClass,
                    pc,
                    tgtR,
                    calleesAndCallers
                )
            }

            state.removeCallSite(instantiatedType)
        }
    }

}

object XTACallGraphAnalysisScheduler extends CallGraphAnalysisScheduler {

    // TODO AB handle assignment of initial instantiated types here! (see superclass)

    override def uses: Set[PropertyBounds] =
        super.uses ++ PropertyBounds.ubs(InstantiatedTypes, Callees)

    override def derivesCollaboratively: Set[PropertyBounds] =
        super.derivesCollaboratively ++ PropertyBounds.ubs(InstantiatedTypes)

    override def initializeAnalysis(p: SomeProject): AbstractCallGraphAnalysis = new XTACallGraphAnalysis(p)
}
