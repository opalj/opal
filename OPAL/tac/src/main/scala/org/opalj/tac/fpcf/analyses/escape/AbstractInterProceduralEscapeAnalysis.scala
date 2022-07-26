/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.EUBPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.SomeInterimEP
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaHeapObject
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.EscapeViaStaticField
import org.opalj.br.fpcf.properties.GlobalEscape
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.properties.VirtualMethodEscapeProperty
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.Context
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites

/**
 * Adds inter-procedural behavior to escape analyses.
 * Uses the call graph associated with the [[Callees]] properties.
 * It queries the escape state of the [[org.opalj.br.analyses.VirtualFormalParameter]] of the
 * targets.
 *
 * @author Florian Kübler
 */
trait AbstractInterProceduralEscapeAnalysis extends AbstractEscapeAnalysis {

    override type AnalysisContext <: AbstractEscapeAnalysisContext with PropertyStoreContainer with IsMethodOverridableContainer with VirtualFormalParametersContainer with DeclaredMethodsContainer

    override type AnalysisState <: AbstractEscapeAnalysisState with ReturnValueUseSites

    protected[this] override def handleStaticMethodCall(
        call: StaticMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkCall(call.pc, None, call.params, hasAssignment = false)
    }

    protected[this] override def handleStaticFunctionCall(
        call:          StaticFunctionCall[V],
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkCall(call.pc, None, call.params, hasAssignment)
    }

    protected[this] override def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkCall(call.pc, Some(call.receiver), call.params, hasAssignment = false)
    }

    protected[this] override def handleVirtualFunctionCall(
        call:          VirtualFunctionCall[V],
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkCall(call.pc, Some(call.receiver), call.params, hasAssignment)
    }

    protected[this] override def handleParameterOfConstructor(
        call: NonVirtualMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkCall(call.pc, None, call.params, hasAssignment = false)
    }

    protected[this] override def handleNonVirtualAndNonConstructorCall(
        call: NonVirtualMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkCall(call.pc, Some(call.receiver), call.params, hasAssignment = false)
    }

    protected[this] override def handleNonVirtualFunctionCall(
        call:          NonVirtualFunctionCall[V],
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkCall(call.pc, Some(call.receiver), call.params, hasAssignment)
    }

    private[this] def checkCall(
        pc:            Int,
        receiver:      Option[Expr[V]],
        params:        Seq[Expr[V]],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        if (receiver.isDefined && state.usesDefSite(receiver.get)) {
            handleCallForParameter(pc, parameter = 0, hasAssignment)
        }

        for {
            i <- params.indices
            if state.usesDefSite(params(i))
        } handleCallForParameter(pc, i + 1, hasAssignment)

    }

    private[this] def handleCallForParameter(
        pc:            Int,
        parameter:     Int,
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        state.meetMostRestrictive(EscapeInCallee)
        val dm = declaredMethods(context.targetMethod)

        val mostCurrentCalleesEP = propertyStore(dm, Callees.key)
        val calleesEP =
            if (state.containsDependency(mostCurrentCalleesEP))
                state.getDependency(dm).asInstanceOf[EOptionP[DeclaredMethod, Callees]]
            else {
                // we have not yet seen this dependency and it is refinable -> add a dependency
                if (mostCurrentCalleesEP.isRefinable) {
                    state.addDependency(mostCurrentCalleesEP)
                }
                mostCurrentCalleesEP
            }

        if (calleesEP.hasUBP) {
            val callees = calleesEP.ub

            if (callees.isIncompleteCallSite(context.entity._1, pc)) {
                state.meetMostRestrictive(AtMost(EscapeInCallee))
            }
            for (callee <- callees.directCallees(context.entity._1, pc)) {
                val fps = context.virtualFormalParameters(callee.method)

                // there is a call to a method out of the analysis' scope
                if (fps == null) {
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                } else if (project.isSignaturePolymorphic(
                    callee.method.definedMethod.classFile.thisType,
                    callee.method.definedMethod
                )) {
                    // IMPROVE: Signature polymorphic methods like invoke(Exact) do not escape their
                    // parameters directly and indirect effects are handled by the indirect callees
                    // code below
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                } else {
                    val fp = fps(parameter)
                    if (fp != context.entity._2)
                        handleEscapeState((callee, fp), hasAssignment)
                }
            }

            /*
             * For indirect callees, e.g. reflective calls, we only check whether any of the
             * actual parameters of the indirectly called method may be the current entity.
             * Thus, we do not track arrays or fields here. This is currently sound, as we do not
             * support any array or field, and handle them conservatively.
             */
            for {
                indirectCallee <- callees.indirectCallees(context.entity._1, pc)
                indirectCallReceiver = callees.indirectCallReceiver(context.entity._1, pc, indirectCallee)
                indirectCallParams = callees.indirectCallParameters(context.entity._1, pc, indirectCallee)
                (Some(uvar), i) <- (indirectCallReceiver +: indirectCallParams).zipWithIndex
                indirectCallParam = uVarForDefSites(uvar, state.tacai.get.pcToIndex)
                if state.usesDefSite(indirectCallParam)
            } {
                val fps = context.virtualFormalParameters(indirectCallee.method)
                // there is a call to a method out of the analysis' scope
                if (fps == null)
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                else {
                    val fp = fps(i)
                    // fp may be null if the indirect method is static and the parameter is this
                    if (fp != null && fp != context.entity._2)
                        handleEscapeState((indirectCallee, fp), hasAssignment)
                }
            }

        }
    }

    private[this] def handleEscapeState(
        fp:            (Context, VirtualFormalParameter),
        hasAssignment: Boolean
    )(
        implicit
        state: AnalysisState
    ): Unit = {
        /*
         * Handling a escape state twice, does not affect the escape state
         */
        val escapeState = propertyStore(fp, EscapeProperty.key)
        if (state.containsDependency(escapeState)) {
            if (hasAssignment && !(state.hasReturnValueUseSites contains fp))
                state.hasReturnValueUseSites += fp
        } else {
            handleEscapeState(escapeState, hasAssignment)
        }
    }

    private[this] def caseConditionalNoEscape(
        ep:            EOptionP[Entity, Property],
        hasAssignment: Boolean
    )(
        implicit
        state: AnalysisState
    ): Unit = {
        state.meetMostRestrictive(EscapeInCallee)

        if (hasAssignment)
            state.hasReturnValueUseSites += ep.e.asInstanceOf[(Context, VirtualFormalParameter)]

        state.addDependency(ep)
    }

    private[this] def handleEscapeState(
        escapeState:   EOptionP[Entity, Property],
        hasAssignment: Boolean
    )(
        implicit
        state: AnalysisState
    ): Unit = {
        val e = escapeState.e.asInstanceOf[(Context, VirtualFormalParameter)]
        escapeState match {
            case FinalP(NoEscape | VirtualMethodEscapeProperty(NoEscape)) =>
                state.meetMostRestrictive(EscapeInCallee)

            case FinalP(EscapeInCallee | VirtualMethodEscapeProperty(EscapeInCallee)) =>
                state.meetMostRestrictive(EscapeInCallee)

            case FinalP(GlobalEscape | VirtualMethodEscapeProperty(GlobalEscape)) =>
                state.meetMostRestrictive(GlobalEscape)

            case FinalP(EscapeViaStaticField | VirtualMethodEscapeProperty(EscapeViaStaticField)) =>
                state.meetMostRestrictive(EscapeViaStaticField)

            case FinalP(EscapeViaHeapObject | VirtualMethodEscapeProperty(EscapeViaHeapObject)) =>
                state.meetMostRestrictive(EscapeViaHeapObject)

            case FinalP(EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn)) if hasAssignment =>
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case FinalP(EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn)) =>
                state.meetMostRestrictive(EscapeInCallee)

            // we do not track parameters or exceptions in the callee side
            case FinalP(p) if !p.isInstanceOf[AtMost] =>
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case FinalP(AtMost(_) | VirtualMethodEscapeProperty(AtMost(_))) =>
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case FinalP(p) =>
                throw new UnknownError(s"unexpected escape property ($p) for $e")

            case ep @ InterimUBP(AtMost(_) | VirtualMethodEscapeProperty(AtMost(_))) =>
                state.meetMostRestrictive(AtMost(EscapeInCallee))
                state.addDependency(ep)

            case ep @ InterimUBP(EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn)) =>
                if (hasAssignment) {
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                    state.hasReturnValueUseSites += e
                } else
                    state.meetMostRestrictive(EscapeInCallee)

                state.addDependency(ep)

            case ep @ InterimUBP(NoEscape | VirtualMethodEscapeProperty(NoEscape)) =>
                caseConditionalNoEscape(ep, hasAssignment)

            case ep @ InterimUBP(EscapeInCallee | VirtualMethodEscapeProperty(EscapeInCallee)) =>
                caseConditionalNoEscape(ep, hasAssignment)

            case ep: SomeInterimEP =>
                state.meetMostRestrictive(AtMost(EscapeInCallee))
                if (hasAssignment)
                    state.hasReturnValueUseSites += e

                state.addDependency(ep)

            case epk =>
                caseConditionalNoEscape(epk, hasAssignment)
        }
    }

    abstract override protected[this] def c(
        someEPS: SomeEPS
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): ProperPropertyComputationResult = {
        someEPS match {
            case EUBPS(_: DeclaredMethod, _: Callees, isFinal) =>
                state.removeDependency(someEPS)
                if (!isFinal) {
                    state.addDependency(someEPS)
                }
                analyzeTAC()

            case EPS((_: Context, VirtualFormalParameter(dm: DefinedMethod, -1))) if dm.definedMethod.isConstructor =>
                throw new RuntimeException("can't handle the this-reference of the constructor")

            case EPS(other: (_, _)) if other._2.isInstanceOf[VirtualFormalParameter] =>
                state.removeDependency(someEPS)
                handleEscapeState(
                    someEPS,
                    state.hasReturnValueUseSites contains
                        other.asInstanceOf[(Context, VirtualFormalParameter)]
                )
                returnResult

            case _ => super.c(someEPS)

        }
    }
}
