/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.fpcf.cg.properties.Callees
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.VirtualMethodEscapeProperty
import org.opalj.tac.Expr
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.UVar
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

/**
 * Adds inter-procedural behavior to escape analyses.
 * Uses the call graph associated with the [[org.opalj.fpcf.cg.properties.Callees]] properties.
 * It queries the escape state of the [[org.opalj.br.analyses.VirtualFormalParameter]] of the
 * targets.
 *
 * @author Florian Kübler
 */
trait AbstractInterProceduralEscapeAnalysis extends AbstractEscapeAnalysis {

    override type AnalysisContext <: AbstractEscapeAnalysisContext with PropertyStoreContainer with IsMethodOverridableContainer with VirtualFormalParametersContainer with DeclaredMethodsContainer

    override type AnalysisState <: AbstractEscapeAnalysisState with DependeeCache with ReturnValueUseSites

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
            i ← params.indices
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
        val calleesEP = state.calleesCache.getOrElseUpdate(dm, propertyStore(dm, Callees.key))
        if (calleesEP.isRefinable) {
            state.addDependency(calleesEP)
        }
        if (calleesEP.hasProperty) {
            val callees = calleesEP.ub

            if (callees.isIncompleteCallSite(pc)) {
                state.meetMostRestrictive(AtMost(EscapeInCallee))
            }
            for (callee ← callees.directCallees(pc)) {
                val fps = context.virtualFormalParameters(callee)

                // there is a call to a method out of the analysis' scope
                if (fps == null)
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                else
                    handleEscapeState(fps(parameter), hasAssignment)
            }

            /* 
             * For indirect callees, e.g. reflective calls, we only check whether any of the 
             * actual parameters of the indirectly called method may be the current entity.
             * Thus, we do not track arrays or fields here. This is currently sound, as we do not
             * support any array or field, and handle them conservatively.
             */
            for {
                indirectCallee ← callees.indirectCallees(pc)
                // parameters(0) is the param of the defSite
                indirectCallParams = callees.indirectCallParameters(pc, indirectCallee)
                (Some((value, defSites)), i) ← indirectCallParams.zipWithIndex
                indirectCallParam = UVar(value, defSites.map(x ⇒ state.tacai.get.pcToIndex(x)))
                if state.usesDefSite(indirectCallParam)
            } {
                val fps = context.virtualFormalParameters(indirectCallee)
                handleEscapeState(fps(i), hasAssignment)
            }

        }
    }

    private[this] def handleEscapeState(
        fp:            VirtualFormalParameter,
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        /* This is crucial for the analysis. the dependees set is not allowed to
         * contain duplicates. Due to very long target methods it could be the case
         * that multiple queries to the property store result in either an EP or an
         * EPK. Therefore we cache the result to have it consistent.
         */
        val escapeState = state.dependeeCache.getOrElseUpdate(fp, context.propertyStore(fp, EscapeProperty.key))
        //        val escapeState = if (isConcreteMethod) {
        //            state.dependeeCache.getOrElseUpdate(fp, context.propertyStore(fp, EscapeProperty.key))
        //        } else {
        //            state.vdependeeCache.getOrElseUpdate(
        //                fp, context.propertyStore(fp, VirtualMethodEscapeProperty.key)
        //            )
        //        }
        handleEscapeState(escapeState, hasAssignment)
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
            state.hasReturnValueUseSites += ep.e.asInstanceOf[VirtualFormalParameter]

        state.addDependency(ep)
    }

    private[this] def handleEscapeState(
        escapeState:   EOptionP[Entity, Property],
        hasAssignment: Boolean
    )(
        implicit
        state: AnalysisState
    ): Unit = {
        assert(escapeState.e.isInstanceOf[VirtualFormalParameter])

        val e = escapeState.e.asInstanceOf[VirtualFormalParameter]
        escapeState match {
            case FinalEP(_, NoEscape | VirtualMethodEscapeProperty(NoEscape)) ⇒
                state.meetMostRestrictive(EscapeInCallee)

            case FinalEP(_, EscapeInCallee | VirtualMethodEscapeProperty(EscapeInCallee)) ⇒
                state.meetMostRestrictive(EscapeInCallee)

            case FinalEP(_, GlobalEscape | VirtualMethodEscapeProperty(GlobalEscape)) ⇒
                state.meetMostRestrictive(GlobalEscape)

            case FinalEP(_, EscapeViaStaticField | VirtualMethodEscapeProperty(EscapeViaStaticField)) ⇒
                state.meetMostRestrictive(EscapeViaStaticField)

            case FinalEP(_, EscapeViaHeapObject | VirtualMethodEscapeProperty(EscapeViaHeapObject)) ⇒
                state.meetMostRestrictive(EscapeViaHeapObject)

            case FinalEP(_, EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn)) if hasAssignment ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case FinalEP(_, EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn)) ⇒
                state.meetMostRestrictive(EscapeInCallee)

            // we do not track parameters or exceptions in the callee side
            case FinalEP(_, p) if !p.isInstanceOf[AtMost] ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case FinalEP(_, AtMost(_) | VirtualMethodEscapeProperty(AtMost(_))) ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case FinalEP(_, p) ⇒
                throw new UnknownError(s"unexpected escape property ($p) for $e")

            case ep @ IntermediateEP(_, _, AtMost(_) | VirtualMethodEscapeProperty(AtMost(_))) ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))
                state.addDependency(ep)

            case ep @ IntermediateEP(_, _, EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn)) ⇒
                if (hasAssignment) {
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                    state.hasReturnValueUseSites += e
                } else
                    state.meetMostRestrictive(EscapeInCallee)

                state.addDependency(ep)

            case ep @ IntermediateEP(_, _, NoEscape | VirtualMethodEscapeProperty(NoEscape)) ⇒
                caseConditionalNoEscape(ep, hasAssignment)

            case ep @ IntermediateEP(_, _, EscapeInCallee | VirtualMethodEscapeProperty(EscapeInCallee)) ⇒
                caseConditionalNoEscape(ep, hasAssignment)

            case ep @ IntermediateEP(_, _, _) ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))
                if (hasAssignment)
                    state.hasReturnValueUseSites += e

                state.addDependency(ep)

            case epk ⇒
                caseConditionalNoEscape(epk, hasAssignment)
        }
    }

    abstract override protected[this] def continuation(
        someEPS: SomeEPS
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): PropertyComputationResult = {
        if (context.entity.toString.equals("VirtualFormalParameter(org.opalj.fpcf.properties.purity.PurityMatcher{ boolean $anonfun$evaluateEP$6(int,java.lang.String,org.opalj.fpcf.PropertyStore,org.opalj.br.analyses.DeclaredMethods,org.opalj.br.Method) },origin=-3)")) {
            println()
        }
        someEPS match {
            case ESimplePS(dm: DeclaredMethod, _: Callees, isFinal) ⇒
                state.removeDependency(someEPS)
                if (!isFinal) {
                    state.addDependency(someEPS)
                }
                state.calleesCache.update(dm, someEPS.asInstanceOf[EPS[DeclaredMethod, Callees]])
                analyzeTAC()

            case EPS(VirtualFormalParameter(dm: DefinedMethod, -1), _, _) if dm.definedMethod.isConstructor ⇒
                throw new RuntimeException("can't handle the this-reference of the constructor")

            case EPS(other: VirtualFormalParameter, _, _) ⇒
                state.removeDependency(someEPS)
                // todo think about a nicer way and the reason we need this
                state.dependeeCache.update(other, someEPS.asInstanceOf[EPS[Entity, EscapeProperty]])
                handleEscapeState(someEPS, state.hasReturnValueUseSites contains other)
                returnResult

            case _ ⇒ super.continuation(someEPS)

        }
    }
}
