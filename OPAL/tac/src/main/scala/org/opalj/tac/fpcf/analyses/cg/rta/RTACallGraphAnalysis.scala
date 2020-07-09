/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package rta

import scala.language.existentials

import org.opalj.collection.ForeachRefIterator
import org.opalj.fpcf.EPS
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.cg.InstantiatedTypes
import org.opalj.tac.fpcf.properties.TACAI

/**
 * A rapid type call graph analysis (RTA). For a given [[org.opalj.br.Method]] it computes the set
 * of outgoing call edges ([[org.opalj.br.fpcf.properties.cg.Callees]]). Furthermore, it updates the
 * [[org.opalj.br.fpcf.properties.cg.Callers]].
 *
 * This analysis does not handle features such as JVM calls to static initializers or finalize
 * calls.
 * However, analyses for these features (e.g. [[org.opalj.tac.fpcf.analyses.cg.FinalizerAnalysis]]
 * or the [[org.opalj.tac.fpcf.analyses.cg.LoadedClassesAnalysis]]) can be executed within the
 * same batch and the call graph will be generated in collaboration.
 *
 * @author Florian Kuebler
 */
class RTACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractCallGraphAnalysis {

    // TODO maybe cache results for Object.toString, Iterator.hasNext, Iterator.next

    private[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    override type State = RTAState

    override def c(state: RTAState)(eps: SomeEPS): ProperPropertyComputationResult = eps match {
        case UBP(_: InstantiatedTypes) ⇒
            val seenTypes = state.instantiatedTypesUB.size

            state.updateInstantiatedTypesDependee(
                eps.asInstanceOf[EPS[SomeProject, InstantiatedTypes]]
            )

            // we only want to add the new calls, so we create a fresh object
            val calleesAndCallers = new DirectCalls()

            handleVirtualCallSites(calleesAndCallers, seenTypes)(state)

            returnResult(calleesAndCallers)(state)

        case _ ⇒ super.c(state)(eps)
    }

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): RTAState = {
        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)
        new RTAState(definedMethod, tacEP, instantiatedTypesEOptP)
    }

    override def doHandleImpreciseCall(
        caller:                        DefinedMethod,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        potentialTargets:              ForeachRefIterator[ObjectType],
        calleesAndCallers:             DirectCalls
    )(implicit state: RTAState): Unit = {
        for (possibleTgtType ← potentialTargets) {
            if (state.instantiatedTypesUB.contains(possibleTgtType)) {
                val tgtR = project.instanceCall(
                    caller.declaringClassType,
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

    /**
     * Computes the calls from the given method
     * ([[org.opalj.br.fpcf.properties.cg.Callees]] property) and updates the
     * [[org.opalj.br.fpcf.properties.cg.Callers]].
     *
     * Whenever a `declaredMethod` becomes reachable (the caller property is set initially),
     * this method is called.
     * In case the method never becomes reachable, the fallback
     * [[org.opalj.br.fpcf.properties.cg.NoCallers]] will be used by the framework and this method
     * returns [[org.opalj.fpcf.NoResult]].
     */

    // modifies state and the calleesAndCallers
    private[this] def handleVirtualCallSites(
        calleesAndCallers: DirectCalls, seenTypes: Int
    )(implicit state: RTAState): Unit = {
        state.newInstantiatedTypes(seenTypes).filter(_.isObjectType).foreach { instantiatedType ⇒
            val callSites = state.getVirtualCallSites(instantiatedType.asObjectType)
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

            state.removeCallSite(instantiatedType.asObjectType)
        }
    }

}

object RTACallGraphAnalysisScheduler extends CallGraphAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        super.requiredProjectInformation :+ IsOverridableMethodKey

    override def uses: Set[PropertyBounds] = super.uses + PropertyBounds.ub(InstantiatedTypes)

    override def initializeAnalysis(p: SomeProject): AbstractCallGraphAnalysis = new RTACallGraphAnalysis(p)
}
