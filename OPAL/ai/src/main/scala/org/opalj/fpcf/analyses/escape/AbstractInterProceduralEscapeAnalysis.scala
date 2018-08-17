/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package escape

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.VirtualFormalParameter
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
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

/**
 * Adds inter-procedural behavior to escape analyses.
 * Uses the results of the [[org.opalj.fpcf.analyses.VirtualCallAggregatingEscapeAnalysis]]
 * attached to the [[org.opalj.br.analyses.VirtualFormalParameter]] entities.
 *
 * Parameter of non-virtual methods are represented as [[org.opalj.br.analyses.VirtualFormalParameter]]
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
        checkParams(call.resolveCallTarget, call.params, hasAssignment = false)
    }

    protected[this] override def handleStaticFunctionCall(
        call:          StaticFunctionCall[V],
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkParams(call.resolveCallTarget, call.params, hasAssignment)
    }

    protected[this] override def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        handleVirtualCall(
            call.declaringClass,
            call.isInterface,
            call.name,
            call.descriptor,
            call.receiver,
            call.params,
            hasAssignment = false
        )
    }

    protected[this] override def handleVirtualFunctionCall(
        call:          VirtualFunctionCall[V],
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        handleVirtualCall(
            call.declaringClass,
            call.isInterface,
            call.name,
            call.descriptor,
            call.receiver,
            call.params,
            hasAssignment
        )
    }

    protected[this] override def handleParameterOfConstructor(
        call: NonVirtualMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        checkParams(
            call.resolveCallTarget(context.targetMethodDeclaringClassType),
            call.params,
            hasAssignment = false
        )
    }

    protected[this] override def handleNonVirtualAndNonConstructorCall(
        call: NonVirtualMethodCall[V]
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        val methodO = call.resolveCallTarget(context.targetMethodDeclaringClassType)
        checkParams(methodO, call.params, hasAssignment = false)
        if (context.usesDefSite(call.receiver))
            handleCall(methodO, param = 0, hasAssignment = false)
    }

    protected[this] override def handleNonVirtualFunctionCall(
        call:          NonVirtualFunctionCall[V],
        hasAssignment: Boolean
    )(
        implicit
        context: AnalysisContext,
        state:   AnalysisState
    ): Unit = {
        val methodO = call.resolveCallTarget(context.targetMethodDeclaringClassType)
        checkParams(methodO, call.params, hasAssignment)
        if (context.usesDefSite(call.receiver))
            handleCall(methodO, param = 0, hasAssignment = hasAssignment)
    }

    private[this] def handleVirtualCall(
        dc:            ReferenceType,
        isInterface:   Boolean,
        name:          String,
        descr:         MethodDescriptor,
        receiver:      Expr[V],
        params:        Seq[Expr[V]],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(receiver.isVar)
        val targetMethod = context.targetMethod
        val callerType = targetMethod.classFile.thisType
        val value = receiver.asVar.value.asReferenceValue

        val receiverType = value.valueType

        if (receiverType.isEmpty) {
            // Nothing to do
            // the receiver is null, the method is not invoked and the object does not escape
        } else if (receiverType.get.isArrayType) {

            // for arrays we know the concrete method which is defined by java.lang.Object
            val methodO = project.instanceCall(callerType, ObjectType.Object, name, descr)
            checkParams(methodO, params, hasAssignment)
            if (context.usesDefSite(receiver))
                handleCall(methodO, param = 0, hasAssignment = hasAssignment)
        } else if (value.isPrecise) {

            // if the receiver type is precisely known, we can handle the concrete method
            val methodO = project.instanceCall(callerType, receiverType.get, name, descr)

            checkParams(methodO, params, hasAssignment)
            if (context.usesDefSite(receiver))
                handleCall(methodO, param = 0, hasAssignment = hasAssignment)
        } else /* non-null, not precise object type */ {

            val callee =
                declaredMethods(
                    dc.asObjectType,
                    callerType.packageName,
                    receiverType.get.asObjectType,
                    name,
                    descr
                )

            if (!callee.hasSingleDefinedMethod ||
                context.isMethodOverridable(callee.definedMethod).isNotNo) {
                // the type of the virtual call is extensible and the analysis mode is library like
                // therefore the method could be overriden and we do not know if the object escapes
                //
                // to optimize performance, we do not let the analysis run against the existing methods
                state.meetMostRestrictive(AtMost(EscapeInCallee))
            } else {
                val method = callee.definedMethod
                if (project.isSignaturePolymorphic(method.classFile.thisType, method)) {
                    //IMPROVE
                    // check if this is to much (param contains def-site)
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                } else {
                    // handle the receiver
                    if (context.usesDefSite(receiver)) {
                        val fp = context.virtualFormalParameters(callee)
                        assert((fp ne null) && (fp(0) ne null))
                        handleEscapeState(fp(0), hasAssignment, isConcreteMethod = false)

                    }

                    // handle the parameters
                    for (i ← params.indices) {
                        if (context.usesDefSite(params(i))) {
                            val fp = context.virtualFormalParameters(callee)
                            assert((fp ne null) && (fp(i + 1) ne null))
                            handleEscapeState(fp(i + 1), hasAssignment, isConcreteMethod = false)
                        }
                    }
                }
            }
        }
    }

    private[this] def checkParams(
        methodO:       org.opalj.Result[Method],
        params:        Seq[Expr[V]],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        for (i ← params.indices) {
            if (context.usesDefSite(params(i)))
                handleCall(methodO, i + 1, hasAssignment)
        }
    }

    private[this] def handleCall(
        methodO:       org.opalj.Result[Method],
        param:         Int,
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        // we definitively escape into to callee
        state.meetMostRestrictive(EscapeInCallee)
        methodO match {
            case Success(method) ⇒
                if (project.isSignaturePolymorphic(method.classFile.thisType, method)) {
                    //IMPROVE
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                } else {
                    val fp =
                        context.virtualFormalParameters(context.declaredMethods(method))(param)

                    // for self recursive calls, we do not need handle the call any further
                    if (fp != context.entity) {
                        handleEscapeState(fp, hasAssignment, isConcreteMethod = true)
                    }
                }
            case _ ⇒ state.meetMostRestrictive(AtMost(EscapeInCallee))
        }
    }

    private[this] def handleEscapeState(
        fp:               VirtualFormalParameter,
        hasAssignment:    Boolean,
        isConcreteMethod: Boolean
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
        val escapeState = if (isConcreteMethod) {
            state.dependeeCache.getOrElseUpdate(fp, context.propertyStore(fp, EscapeProperty.key))
        } else {
            state.vdependeeCache.getOrElseUpdate(
                fp, context.propertyStore(fp, VirtualMethodEscapeProperty.key)
            )
        }
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

        someEPS.e match {
            case VirtualFormalParameter(DefinedMethod(_, m), -1) if m.isConstructor ⇒
                throw new RuntimeException("can't handle the this-reference of the constructor")

            // this entity is passed as parameter (or this local) to a method
            case other: VirtualFormalParameter ⇒
                state.removeDependency(someEPS)
                handleEscapeState(someEPS, state.hasReturnValueUseSites contains other)
                returnResult

            case _ ⇒ super.continuation(someEPS)
        }
    }
}
