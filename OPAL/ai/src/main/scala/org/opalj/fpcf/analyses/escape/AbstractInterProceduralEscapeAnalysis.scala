/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndReturn
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
 * @author Florian Kuebler
 */
trait AbstractInterProceduralEscapeAnalysis extends AbstractEscapeAnalysis {
    override type AnalysisContext <: AbstractEscapeAnalysisContext with ProjectContainer with PropertyStoreContainer with IsMethodOverridableContainer with VirtualFormalParametersContainer with DeclaredMethodsContainer
    override type AnalysisState <: AbstractEscapeAnalysisState with DependeeCache with ReturnValueUseSites

    protected[this] override def handleStaticMethodCall(
        call: StaticMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleStaticCall(
            call.declaringClass, call.isInterface, call.name, call.descriptor, call.params, hasAssignment = false
        )
    }

    protected[this] override def handleStaticFunctionCall(
        call: StaticFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        handleStaticCall(
            call.declaringClass,
            call.isInterface,
            call.name,
            call.descriptor,
            call.params,
            hasAssignment
        )
    }

    protected[this] override def handleVirtualMethodCall(
        call: VirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
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
        call: VirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
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
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        val methodO = context.project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params, hasAssignment = false)
    }

    protected[this] override def handleNonVirtualAndNonConstructorCall(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        val methodO = context.project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params, hasAssignment = false)
        if (context.usesDefSite(call.receiver))
            handleCall(methodO, 0, hasAssignment = false)
    }

    protected[this] override def handleNonVirtualFunctionCall(
        call: NonVirtualFunctionCall[V], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        val methodO = context.project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params, hasAssignment)
        if (context.usesDefSite(call.receiver))
            handleCall(methodO, 0, hasAssignment)
    }

    private[this] def handleStaticCall(
        dc:            ReferenceType,
        isI:           Boolean,
        name:          String,
        descr:         MethodDescriptor,
        params:        Seq[Expr[V]],
        hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        checkParams(
            context.project.staticCall(dc.asObjectType, isI, name, descr), params, hasAssignment
        )
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

        val project = context.project
        val targetMethod = context.targetMethod

        val value = receiver.asVar.value.asDomainReferenceValue

        if (value.isNull.isYes) {
            // the receiver is null, the method is not invoked and the object does not escape
            return
        }

        val receiverType = project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
            value.upperTypeBound
        )
        if (receiverType.isArrayType) {

            // for arrays we know the concrete method which is defined by java.lang.Object
            val methodO = project.instanceCall(
                targetMethod.declaringClassType.asObjectType, ObjectType.Object, name, descr
            )
            checkParams(methodO, params, hasAssignment)
            if (context.usesDefSite(receiver))
                handleCall(methodO, 0, hasAssignment)
        } else if (value.isPrecise) {

            // if the receiver type is precisely known, we can handle the concrete method
            val valueType = value.valueType.get
            assert(targetMethod.declaringClassType.isObjectType)
            var methodO = project.instanceCall(
                targetMethod.declaringClassType.asObjectType, valueType, name, descr
            )

            // check if the method is abstract?
            if (methodO.isEmpty) {
                project.classFile(receiverType.asObjectType) match {
                    case Some(cf) ⇒
                        methodO = if (cf.isInterfaceDeclaration) {
                            org.opalj.Result(
                                project.resolveInterfaceMethodReference(
                                    valueType.asObjectType, name, descr
                                )
                            )
                        } else {
                            project.resolveClassMethodReference(valueType.asObjectType, name, descr)
                        }
                    case None ⇒
                        state.meetMostRestrictive(AtMost(EscapeInCallee))
                        return
                }
            }

            checkParams(methodO, params, hasAssignment)
            if (context.usesDefSite(receiver))
                handleCall(methodO, 0, hasAssignment)
        } else /* non-null, not precise object type */ {

            val target = project.instanceCall(
                targetMethod.declaringClassType.asObjectType, receiverType, name, descr
            )

            // did we found a method and is this method not overridable?
            if (target.isEmpty || context.isMethodOverridable(target.value).isNotNo) {
                // the type of the virtual call is extensible and the analysis mode is library like
                // therefore the method could be overriden and we do not know if the object escapes
                //
                // to optimize performance, we do not let the analysis run against the existing methods
                state.meetMostRestrictive(AtMost(EscapeInCallee))
            } else {
                val dm = DefinedMethod(receiverType, target.value)
                assert(dm ne null)
                if (project.isSignaturePolymorphic(dm.definedMethod.classFile.thisType, dm.definedMethod)) {
                    //IMPROVE
                    // check if this is to much (param contains def-site)
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                } else {
                    // handle the receiver
                    if (context.usesDefSite(receiver)) {
                        val fp = context.virtualFormalParameters(dm)
                        assert((fp ne null) && (fp(0) ne null))
                        handleEscapeState(fp(0), hasAssignment, isConcreteMethod = false)

                    }

                    // handle the parameters
                    for (i ← params.indices) {
                        if (context.usesDefSite(params(i))) {
                            val fp = context.virtualFormalParameters(dm)
                            assert((fp ne null) && (fp(i + 1) ne null))
                            handleEscapeState(fp(i + 1), hasAssignment, isConcreteMethod = false)
                        }
                    }
                }

            }
        }
    }

    private[this] def checkParams(
        methodO: org.opalj.Result[Method], params: Seq[Expr[V]], hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        for (i ← params.indices) {
            if (context.usesDefSite(params(i)))
                handleCall(methodO, i + 1, hasAssignment)
        }
    }

    private[this] def handleCall(
        methodO: org.opalj.Result[Method], param: Int, hasAssignment: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        methodO match {
            case Success(method) ⇒
                if (context.project.isSignaturePolymorphic(method.classFile.thisType, method)) {
                    //IMPROVE
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                } else {
                    val fp = context.virtualFormalParameters(context.declaredMethods(method))(param)

                    // for self recursive calls, we do not need handle the call any further
                    if (fp != context.entity) {
                        handleEscapeState(fp, hasAssignment, isConcreteMethod = true)
                    }
                }
            case _ ⇒ state.meetMostRestrictive(AtMost(EscapeInCallee))
        }
    }

    private[this] def handleEscapeState(
        fp: VirtualFormalParameter, hasAssignment: Boolean, isConcreteMethod: Boolean
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
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

    private[this] def handleEscapeState(
        escapeState: EOptionP[Entity, Property], hasAssignment: Boolean
    )(implicit state: AnalysisState): Unit = {
        escapeState match {
            case EP(_, NoEscape | VirtualMethodEscapeProperty(NoEscape)) ⇒
                state.meetMostRestrictive(EscapeInCallee)

            case EP(_, EscapeInCallee | VirtualMethodEscapeProperty(EscapeInCallee)) ⇒
                state.meetMostRestrictive(EscapeInCallee)

            case EP(_, GlobalEscape | VirtualMethodEscapeProperty(GlobalEscape)) ⇒
                state.meetMostRestrictive(GlobalEscape)

            case EP(_, EscapeViaStaticField | VirtualMethodEscapeProperty(EscapeViaStaticField)) ⇒
                state.meetMostRestrictive(EscapeViaStaticField)

            case EP(_, EscapeViaHeapObject | VirtualMethodEscapeProperty(EscapeViaHeapObject)) ⇒
                state.meetMostRestrictive(EscapeViaHeapObject)

            case EP(_, EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn)) if hasAssignment ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case EP(_, EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn)) ⇒
                state.meetMostRestrictive(EscapeInCallee)

            // we do not track parameters or exceptions in the callee side
            case EP(_, p) if p.isFinal ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case EP(_, AtMost(_) | VirtualMethodEscapeProperty(AtMost(_))) ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))

            case ep @ EP(_, Conditional(AtMost(_)) | VirtualMethodEscapeProperty(Conditional(AtMost(_)))) ⇒
                state.meetMostRestrictive(AtMost(EscapeInCallee))

                state.dependees += ep
            case ep @ EP(e, Conditional(EscapeViaReturn) | VirtualMethodEscapeProperty(Conditional(EscapeViaReturn))) ⇒

                if (hasAssignment) {
                    state.meetMostRestrictive(AtMost(EscapeInCallee))
                    state.hasReturnValueUseSites += e
                } else
                    state.meetMostRestrictive(EscapeInCallee)

                state.dependees += ep

            case ep @ EP(e, Conditional(p)) ⇒
                state.meetMostRestrictive(EscapeInCallee meet p)

                if (hasAssignment)
                    state.hasReturnValueUseSites += e

                state.dependees += ep

            case ep @ EP(e, VirtualMethodEscapeProperty(Conditional(p))) ⇒
                state.meetMostRestrictive(EscapeInCallee meet p)

                if (hasAssignment)
                    state.hasReturnValueUseSites += e

                state.dependees += ep

            case epkOrConditional ⇒
                state.meetMostRestrictive(EscapeInCallee)

                if (hasAssignment)
                    state.hasReturnValueUseSites += epkOrConditional.e

                state.dependees += epkOrConditional
        }
    }

    abstract override protected[this] def c(
        other: Entity, p: Property, u: UpdateType
    )(implicit context: AnalysisContext, state: AnalysisState): PropertyComputationResult = {
        other match {
            case VirtualFormalParameter(DefinedMethod(_, m), -1) if m.isConstructor ⇒
                throw new RuntimeException("can't handle the this-reference of the constructor")

            // this entity is passed as parameter (or this local) to a method
            case _: VirtualFormalParameter ⇒ p match {

                case GlobalEscape | VirtualMethodEscapeProperty(GlobalEscape) ⇒
                    Result(context.entity, GlobalEscape)

                case EscapeViaStaticField | VirtualMethodEscapeProperty(EscapeViaStaticField) ⇒
                    Result(context.entity, EscapeViaStaticField)

                case EscapeViaHeapObject | VirtualMethodEscapeProperty(EscapeViaHeapObject) ⇒
                    Result(context.entity, EscapeViaHeapObject)

                case NoEscape | VirtualMethodEscapeProperty(NoEscape) ⇒
                    removeFromDependeesAndComputeResult(EP(other, p), EscapeInCallee)

                case EscapeInCallee | VirtualMethodEscapeProperty(EscapeInCallee) ⇒
                    removeFromDependeesAndComputeResult(EP(other, p), EscapeInCallee)

                case EscapeViaReturn | VirtualMethodEscapeProperty(EscapeViaReturn) ⇒
                    /*
                     * IMPROVE we do not further track the return value of the callee.
                     * But the org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
                     * eliminates the assignments, if the function called is identity-like
                     */
                    if (state.hasReturnValueUseSites contains other)
                        removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))
                    else
                        removeFromDependeesAndComputeResult(EP(other, p), EscapeInCallee)

                case EscapeViaParameter | VirtualMethodEscapeProperty(EscapeViaParameter) ⇒
                    // IMPROVE we do not further track the field of the actual parameter
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))

                case EscapeViaAbnormalReturn | VirtualMethodEscapeProperty(EscapeViaAbnormalReturn) ⇒
                    // IMPROVE we do not further track the exception thrown in the callee
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))

                case EscapeViaParameterAndAbnormalReturn | VirtualMethodEscapeProperty(EscapeViaParameterAndAbnormalReturn) ⇒
                    // combines the cases above
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))

                case EscapeViaNormalAndAbnormalReturn | VirtualMethodEscapeProperty(EscapeViaNormalAndAbnormalReturn) ⇒
                    // combines the cases above
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))

                case EscapeViaParameterAndReturn | VirtualMethodEscapeProperty(EscapeViaParameterAndReturn) ⇒
                    // combines the cases above
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))

                case EscapeViaParameterAndNormalAndAbnormalReturn | VirtualMethodEscapeProperty(EscapeViaParameterAndNormalAndAbnormalReturn) ⇒
                    // combines the cases above
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))

                case AtMost(_) | VirtualMethodEscapeProperty(AtMost(_)) ⇒
                    removeFromDependeesAndComputeResult(EP(other, p), AtMost(EscapeInCallee))

                case p @ Conditional(NoEscape) ⇒
                    performIntermediateUpdate(EP(other, p), EscapeInCallee)

                case p @ VirtualMethodEscapeProperty(Conditional(NoEscape)) ⇒
                    performIntermediateUpdate(EP(other, p), EscapeInCallee)

                case p @ Conditional(EscapeInCallee) ⇒
                    performIntermediateUpdate(EP(other, p), EscapeInCallee)

                case p @ VirtualMethodEscapeProperty(Conditional(EscapeInCallee)) ⇒
                    performIntermediateUpdate(EP(other, p), EscapeInCallee)

                case p @ Conditional(EscapeViaReturn) ⇒
                    if (state.hasReturnValueUseSites contains other)
                        performIntermediateUpdate(EP(other, p), AtMost(EscapeInCallee))
                    else
                        performIntermediateUpdate(EP(other, p), EscapeInCallee)

                case p @ VirtualMethodEscapeProperty(Conditional(EscapeViaReturn)) ⇒
                    if (state.hasReturnValueUseSites contains other)
                        performIntermediateUpdate(EP(other, p), AtMost(EscapeInCallee))
                    else
                        performIntermediateUpdate(EP(other, p), EscapeInCallee)

                case p @ Conditional(_) ⇒
                    performIntermediateUpdate(EP(other, p), AtMost(EscapeInCallee))

                case p @ VirtualMethodEscapeProperty(Conditional(_)) ⇒
                    performIntermediateUpdate(EP(other, p), AtMost(EscapeInCallee))

                case PropertyIsLazilyComputed ⇒
                    IntermediateResult(
                        context.entity,
                        Conditional(state.mostRestrictiveProperty),
                        state.dependees,
                        c
                    )

                case _ ⇒
                    throw new UnknownError(s"unexpected escape property ($p) for $other")
            }
            case _ ⇒ super.c(other, p, u)
        }
    }
}
