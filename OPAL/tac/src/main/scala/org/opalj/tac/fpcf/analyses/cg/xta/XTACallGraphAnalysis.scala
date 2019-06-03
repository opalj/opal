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
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
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
        // TODO A.B. for XTA, we need to know which entity the update came from
        case EUBP(e: DefinedMethod, _: InstantiatedTypes) ⇒
            // TODO think through the code below...
            val seenTypes = state.ownInstantiatedTypesUB.size

            state.updateOwnInstantiatedTypesDependee(
                eps.asInstanceOf[EPS[DefinedMethod, InstantiatedTypes]]
            )

            // we only want to add the new calls, so we create a fresh object
            val calleesAndCallers = new DirectCalls()

            handleVirtualCallSites(calleesAndCallers, seenTypes)(state)

            returnResult(calleesAndCallers)(state)

        case EUBP(e: DefinedMethod, callees: Callees) =>
            // TODO A.B. how to handle callees update?
            // how can I get the new callees? do I need to?
            returnResult(new DirectCalls)(state)

        case _ ⇒ super.c(state)(eps)
    }

    def getCalleeDefinedMethods(callees: Callees): Set[DefinedMethod] = {
        // TODO A.B. check efficiency...
        val calleeMethods = mutable.Set[DefinedMethod]()
        for {
            pc <- callees.callSitePCs
            callee <- callees.callees(pc)
        } {
          calleeMethods += callee.asDefinedMethod
        }
        calleeMethods.toSet
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): XTAState = {
        // TODO A.B. for XTA, do we even know these?
        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(definedMethod, InstantiatedTypes.key)
        val calleesEOptP = propertyStore(definedMethod, Callees.key)
        new XTAState(definedMethod, tacEP, instantiatedTypesEOptP, calleesEOptP, Map())
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

    override def uses: Set[PropertyBounds] = super.uses + PropertyBounds.ub(InstantiatedTypes)

    override def initializeAnalysis(p: SomeProject): AbstractCallGraphAnalysis = new XTACallGraphAnalysis(p)
}
