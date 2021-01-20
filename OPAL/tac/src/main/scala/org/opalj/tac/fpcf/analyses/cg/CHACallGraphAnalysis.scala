/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.collection.ForeachRefIterator
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.tac.fpcf.properties.TACAI

class CHAState(
        val method:                                DefinedMethod,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
) extends CGState {

    override def hasNonFinalCallSite: Boolean = false
}

/**
 * A call graph based on Class Hierarchy Analysis (CHA).
 * Virtual calls are resolved to all methods matching the declared type, name and descriptor of the
 * call. Type information from abstract interpretation is taken into account, however.
 *
 * @author Florian Kuebler
 */
class CHACallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends AbstractCallGraphAnalysis {
    override type State = CHAState

    private[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    override def doHandleImpreciseCall(
        caller:                        DefinedMethod,
        call:                          Call[V] with VirtualCall[V],
        pc:                            Int,
        specializedDeclaringClassType: ReferenceType,
        potentialTargets:              ForeachRefIterator[ObjectType],
        calleesAndCallers:             DirectCalls
    )(implicit state: CHAState): Unit = {
        for (tgt ← potentialTargets) {
            val tgtR = project.instanceCall(
                caller.declaringClassType, tgt, call.name, call.descriptor
            )
            handleCall(
                caller, call.name, call.descriptor, call.declaringClass, pc, tgtR, calleesAndCallers
            )
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

    override def createInitialState(
        definedMethod: DefinedMethod, tacEP: EPS[Method, TACAI]
    ): CHAState = {
        new CHAState(definedMethod, tacEP)
    }
}

object CHACallGraphAnalysisScheduler extends CallGraphAnalysisScheduler {
    override def initializeAnalysis(p: SomeProject): AbstractCallGraphAnalysis = new CHACallGraphAnalysis(p)
}
