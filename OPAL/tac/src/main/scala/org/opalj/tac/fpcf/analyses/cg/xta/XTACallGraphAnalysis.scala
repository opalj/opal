/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.collection.ForeachRefIterator
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
            val allCallees = getCalleeDefinedMethods(callees)

            val alreadySeenCallees = state.seenCallees

            val newCallees = allCallees diff alreadySeenCallees

            // TODO AB improve this
            state.updateSeenCallees(allCallees)

            for (newCallee ← newCallees) {
                // TODO AB handle forward and backward flow for new callee!
                val calleeInstantiatedTypesDependee = propertyStore(newCallee, InstantiatedTypes.key)
                state.updateCalleeInstantiatedTypesDependee(calleeInstantiatedTypesDependee)
            }

            returnResult(new DirectCalls)(state)

        case _ ⇒ super.c(state)(eps)
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

        // TODO AB forward flow (to callees) should be handled here!

        val seenTypes = state.ownInstantiatedTypesUB.size

        state.updateOwnInstantiatedTypesDependee(eps)

        // we only want to add the new calls, so we create a fresh object
        val calleesAndCallers = new DirectCalls()

        handleVirtualCallSites(calleesAndCallers, seenTypes)(state)

        returnResult(calleesAndCallers)(state)
    }

    def handleUpdateOfCalleeTypeSet(state: XTAState, eps: EPS[DefinedMethod, InstantiatedTypes]): ProperPropertyComputationResult = {

        val updatedCallee = eps.e
        val seenTypes = state.calleeSeenTypes(updatedCallee)

        state.updateCalleeInstantiatedTypesDependee(eps)

        val newTypes = eps.ub.dropOldest(seenTypes)

        // TODO AB Check if this is sufficient.
        // TODO AB No backward flow if the return value is not used in state.method.
        val returnTypeOfCallee = updatedCallee.definedMethod.returnType.asObjectType
        newTypes.filter(t ⇒ t.isSubtypeOf(returnTypeOfCallee))

        if (newTypes.nonEmpty) {
            Results(
                InterimPartialResult(state.dependees, c(state)),
                typeFlowPartialResult(state.method, UIDSet(newTypes.toSeq: _*))
            )
        } else {
            InterimPartialResult(state.dependees, c(state))
        }

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
