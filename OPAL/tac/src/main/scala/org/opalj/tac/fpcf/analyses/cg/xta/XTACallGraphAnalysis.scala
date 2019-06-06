/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.DefinedMethod
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.collection.ForeachRefIterator
import org.opalj.collection.immutable.RefArray
import org.opalj.collection.immutable.UIDSet
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
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.fpcf.properties.TACAI

import scala.collection.mutable

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
        val calleeInstantiatedTypesDependee = propertyStore(newCallee, InstantiatedTypes.key)
        state.updateCalleeInstantiatedTypesDependee(calleeInstantiatedTypesDependee)

        if (calleeInstantiatedTypesDependee.hasUBP) {
            val forwardResult = forwardFlow(state.method, newCallee, state.ownInstantiatedTypesUB)
            val backwardResult = backwardFlow(state.method, newCallee, calleeInstantiatedTypesDependee.ub.types)

            forwardResult ++ backwardResult
        } else {
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
        // TODO AB think through the code below...

        val seenTypes = state.ownInstantiatedTypesUB.size

        state.updateOwnInstantiatedTypesDependee(eps)

        // we only want to add the new calls, so we create a fresh object
        val calleesAndCallers = new DirectCalls()

        handleVirtualCallSites(calleesAndCallers, seenTypes)(state)

        // TODO AB forward flow (to callees) should be handled here!
        val newTypes = state.newInstantiatedTypes(seenTypes)
        val forwardFlowResults = state.seenCallees.flatMap(c ⇒ forwardFlow(state.method, c, UIDSet(newTypes.toSeq: _*)))

        returnResult(calleesAndCallers, forwardFlowResults)(state)
    }

    // TODO AB (mostly) copied from super-class since we need to make some adjustments
    override protected def returnResult(
                                calleesAndCallers: DirectCalls
                              )(implicit state: State): ProperPropertyComputationResult = {
        val results = calleesAndCallers.partialResults(state.method)

        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(state)),
                results
            )
        else
            Results(results)
    }

    // TODO (mostly) copied from super-class
    protected def returnResult(calleesAndCallers: DirectCalls, typeFlowPartialResults: TraversableOnce[PartialResult[DefinedMethod, InstantiatedTypes]])(implicit state: State): ProperPropertyComputationResult = {
        val results = calleesAndCallers.partialResults(state.method)

        // TODO AB is it possible to have no open dependencies in XTA? i don't think so!
        // NOTE AB removed "state.hasNonFinalCallSite &&"
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, c(state)),
                // TODO AB not efficient?
                results.toIterator ++ typeFlowPartialResults
            )
        else
            Results(results.toIterator ++ typeFlowPartialResults)
    }

    def handleUpdateOfCalleeTypeSet(state: XTAState, eps: EPS[DefinedMethod, InstantiatedTypes]): ProperPropertyComputationResult = {

        // If a callee has type updates, maybe these new types flow back to this method (the caller).

        val updatedCallee = eps.e
        val seenTypes = state.calleeSeenTypes(updatedCallee)

        state.updateCalleeInstantiatedTypesDependee(eps)

        val newCalleeTypes = eps.ub.dropOldest(seenTypes)

        val backwardFlowResult = backwardFlow(state.method, updatedCallee, UIDSet(newCalleeTypes.toSeq: _*))

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

        val allParameterTypes: RefArray[FieldType] = calleeMethod.definedMethod.parameterTypes
        // TODO AB handle other types than ObjectTypes (e.g. arrays, primitive types like int?)
        // TODO AB performance...; maybe cache this stuff somehow
        val relevantParameterTypes = allParameterTypes.toSeq.filter(_.isObjectType).map(_.asObjectType)

        val newTypes = newCallerTypes.filter(t ⇒ relevantParameterTypes.exists(p ⇒ t.isSubtypeOf(p)))

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(calleeMethod, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    // calculate flow of new types from callee to caller
    private def backwardFlow(callerMethod: DefinedMethod, calleeMethod: DefinedMethod, newCalleeTypes: UIDSet[ObjectType]): Option[PartialResult[DefinedMethod, InstantiatedTypes]] = {

        val returnTypeOfCallee = calleeMethod.definedMethod.returnType
        if (!returnTypeOfCallee.isObjectType) {
            return None;
        }

        val newTypes = newCalleeTypes.filter(t ⇒ t.isSubtypeOf(returnTypeOfCallee.asObjectType))

        if (newTypes.nonEmpty)
            Some(typeFlowPartialResult(callerMethod, UIDSet(newTypes.toSeq: _*)))
        else
            None
    }

    def typeFlowPartialResult(method: DefinedMethod, newTypes: UIDSet[ObjectType]): PartialResult[DefinedMethod, InstantiatedTypes] = {
        PartialResult[DefinedMethod, InstantiatedTypes](
            method,
            InstantiatedTypes.key,
            updateInstantiatedTypes(method, newTypes)
        )
    }

    // for now: mostly copied from InstantiatedTypesAnalysis
    def updateInstantiatedTypes(
        method:               DefinedMethod,
        newInstantiatedTypes: UIDSet[ObjectType]
    )(
        eop: EOptionP[DefinedMethod, InstantiatedTypes]
    ): Option[EPS[DefinedMethod, InstantiatedTypes]] = eop match {
        case InterimUBP(ub: InstantiatedTypes) ⇒
            val newUB = ub.updated(newInstantiatedTypes)
            if (newUB.types.size > ub.types.size)
                Some(InterimEUBP(method, newUB))
            else
                None

        case _: EPK[_, _] ⇒
            throw new IllegalStateException(
                "the instantiated types property should be pre initialized"
            )

        case r ⇒ throw new IllegalStateException(s"unexpected previous result $r")
    }

    def getCalleeDefinedMethods(callees: Callees): Set[DefinedMethod] = {
        // TODO AB check efficiency...
        val calleeMethods = mutable.Set[DefinedMethod]()
        for {
            pc ← callees.callSitePCs
            callee ← callees.callees(pc)
        } {
            calleeMethods += callee.asDefinedMethod
        }
        calleeMethods.toSet
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): XTAState = {
        // TODO AB for XTA, do we even know these?
        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(definedMethod, InstantiatedTypes.key)
        val calleesEOptP = propertyStore(definedMethod, Callees.key)
        new XTAState(definedMethod, tacEP, instantiatedTypesEOptP, calleesEOptP)
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

    override def uses: Set[PropertyBounds] = super.uses ++ PropertyBounds.ubs(InstantiatedTypes, Callees)

    override def initializeAnalysis(p: SomeProject): AbstractCallGraphAnalysis = new XTACallGraphAnalysis(p)
}
