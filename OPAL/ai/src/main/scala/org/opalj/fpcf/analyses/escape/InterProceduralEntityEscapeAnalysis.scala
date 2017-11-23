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

import org.opalj.br.ReferenceType
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ExceptionHandlers
import org.opalj.br.VirtualMethod
import org.opalj.br.Method
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cfg.CFG
import org.opalj.ai.Domain
import org.opalj.ai.AIResult
import org.opalj.ai.ValueOrigin
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.VirtualForwardingMethod
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndReturn
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.DUVar
import org.opalj.tac.Parameters
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.Assignment

trait AbstractInterProceduralEntityEscapeAnalysis extends AbstractEntityEscapeAnalysis {

    //TODO Move to non entity based analysis
    private[this] val isMethodExtensible: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    // STATE MUTATED DURING THE ANALYSIS
    private[this] val dependeeCache: scala.collection.mutable.Map[Entity, EOptionP[Entity, EscapeProperty]] = scala.collection.mutable.Map()
    private[this] var dependeeToStmt = Map.empty[Entity, Option[Assignment[V]]]

    protected[this] override def handleStaticMethodCall(call: StaticMethodCall[V]): Unit = {
        handleStaticCall(
            call.declaringClass, call.isInterface, call.name, call.descriptor, call.params, None
        )
    }

    protected[this] override def handleStaticFunctionCall(
        call: StaticFunctionCall[V], assignment: Option[Assignment[V]]
    ): Unit = {
        handleStaticCall(
            call.declaringClass,
            call.isInterface,
            call.name,
            call.descriptor,
            call.params,
            assignment
        )
    }

    protected[this] override def handleVirtualMethodCall(call: VirtualMethodCall[V]): Unit = {
        handleVirtualCall(
            call.declaringClass,
            call.isInterface,
            call.name,
            call.descriptor,
            call.receiver,
            call.params,
            None
        )
    }

    protected[this] override def handleVirtualFunctionCall(
        call: VirtualFunctionCall[V], assignment: Option[Assignment[V]]
    ): Unit = {
        handleVirtualCall(
            call.declaringClass,
            call.isInterface,
            call.name,
            call.descriptor,
            call.receiver,
            call.params,
            assignment
        )
    }

    protected[this] override def handleParameterOfConstructor(call: NonVirtualMethodCall[V]): Unit = {
        val methodO = project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params, None)
    }

    protected[this] override def handleNonVirtualAndNonConstructorCall(call: NonVirtualMethodCall[V]): Unit = {
        val methodO = project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params, None)
        if (usesDefSite(call.receiver))
            handleCall(methodO, 0, None)
    }

    protected[this] override def handleNonVirtualFunctionCall(
        call: NonVirtualFunctionCall[V], assignment: Option[Assignment[V]]
    ): Unit = {
        val methodO = project.specialCall(
            call.declaringClass.asObjectType,
            call.isInterface,
            call.name,
            call.descriptor
        )
        checkParams(methodO, call.params, assignment)
        if (usesDefSite(call.receiver))
            handleCall(methodO, 0, assignment)
    }

    private[this] def handleStaticCall(
        dc:         ReferenceType,
        isI:        Boolean,
        name:       String,
        descr:      MethodDescriptor,
        params:     Seq[Expr[V]],
        assignment: Option[Assignment[V]]
    ): Unit = {
        checkParams(project.staticCall(dc.asObjectType, isI, name, descr), params, assignment)
    }

    private[this] def handleVirtualCall(
        dc:         ReferenceType,
        isI:        Boolean,
        name:       String,
        descr:      MethodDescriptor,
        receiver:   Expr[V],
        params:     Seq[Expr[V]],
        assignment: Option[Assignment[V]]
    ): Unit = {
        assert(receiver.isVar)
        val value = receiver.asVar.value.asDomainReferenceValue
        if (value.isPrecise) {
            if (value.isNull.isNoOrUnknown) {
                val valueType = value.valueType.get
                assert(m.declaringClassType.isObjectType)
                val methodO = project.instanceCall(m.declaringClassType.asObjectType, valueType, name, descr)
                checkParams(methodO, params, assignment)
                if (usesDefSite(receiver)) handleCall(methodO, 0, assignment)
            } else {
                // the receiver is null, the method is not invoked and the object does not escape
            }
        } else {
            assert(m.declaringClassType.isObjectType)
            val methodO = project.instanceCall(m.declaringClassType.asObjectType, dc, name, descr)
            if (methodO.isEmpty ||
                (dc.isObjectType &&
                    isMethodExtensible(methodO.value).isNotNo &&
                    AnalysisModes.isLibraryLike(project.analysisMode))) {
                // the type of the virtual call is extensible and the analysis mode is library like
                // therefore the method could be overriden and we do not know if the object escapes
                // TODO: to optimize performance, we do not let the analysis run against the existing methods
                calcMostRestrictive(AtMost(EscapeInCallee))
            } else if (dc.isArrayType) {
                val methodO = project.instanceCall(ObjectType.Object, ObjectType.Object, name, descr)
                checkParams(methodO, params, assignment)
                if (usesDefSite(receiver)) handleCall(methodO, 0, assignment)
            } else {
                assert(dc.isObjectType)
                val target = project.instanceCall(
                    m.declaringClassType.asObjectType,
                    dc,
                    name,
                    descr
                )
                if (target.hasValue) {
                    val vm = VirtualForwardingMethod(dc, name, descr, target.value)
                    if (isSignaturePolymorphicMethod(vm.target)) {
                        //IMPROVE
                        calcMostRestrictive(AtMost(EscapeInCallee))
                    } else {
                        if (usesDefSite(receiver)) {
                            val fp = virtualFormalParameters(vm)
                            handleEscapeState(fp(0), assignment)

                        }
                        for (i ← params.indices) {
                            if (usesDefSite(params(i))) {
                                val fp = virtualFormalParameters(vm)
                                handleEscapeState(fp(i + 1), assignment)
                            }
                        }
                        val packageName = m.declaringClassType.asObjectType.packageName
                        val methods =
                            if (isI) project.interfaceCall(dc.asObjectType, name, descr)
                            else project.virtualCall(packageName, dc, name, descr)
                        for (method ← methods) {
                            checkParams(Success(method), params, assignment)
                            if (usesDefSite(receiver))
                                handleCall(Success(method), 0, assignment)
                        }
                    }
                } else {
                    // the method is unknown, so we have to stop here
                    calcMostRestrictive(AtMost(EscapeInCallee))
                }

            }
        }

    }

    private[this] def checkParams(
        methodO: org.opalj.Result[Method], params: Seq[Expr[V]], assignment: Option[Assignment[V]]
    ): Unit = {
        for (i ← params.indices) {
            if (usesDefSite(params(i)))
                handleCall(methodO, i + 1, assignment)
        }
    }

    private[this] def isSignaturePolymorphicMethod(method: Method): Boolean = {
        method.isNativeAndVarargs &&
            method.descriptor.parametersCount == 1 &&
            method.descriptor.parameterType(0).isArrayType &&
            method.descriptor.parameterType(0).asArrayType.componentType == ObjectType.Object &&
            ((method.classFile.thisType eq ObjectType.VarHandle) ||
                (method.classFile.thisType eq ObjectType.MethodHandle))
    }

    private[this] def handleCall(
        methodO: org.opalj.Result[Method], param: Int, assignment: Option[Assignment[V]]
    ): Unit = {
        methodO match {
            case Success(method) ⇒
                // handle signature polymorphic methods
                if (isSignaturePolymorphicMethod(method)) {
                    //IMPROVE
                    calcMostRestrictive(AtMost(EscapeInCallee))
                } else {
                    val fp = formalParameters(method)(param)
                    if (fp != e) {
                        handleEscapeState(fp, assignment)
                    }
                }
            case _ ⇒ calcMostRestrictive(AtMost(EscapeInCallee))
        }
    }

    private[this] def handleEscapeState(fp: Entity, assignment: Option[Assignment[V]]): Unit = {
        /* This is crucial for the analysis. the dependees set is not allowed to
         * contain duplicates. Due to very long target methods it could be the case
         * that multiple queries to the property store result in either an EP or an
         * EPK. Therefore we cache the result to have it consistent.
         */
        //TODO use mutable map
        val escapeState = dependeeCache.getOrElse(fp, {
            val es = propertyStore(fp, EscapeProperty.key)
            dependeeCache += ((fp, es))
            es
        })

        escapeState match {
            case EP(_, NoEscape | EscapeInCallee) ⇒ calcMostRestrictive(EscapeInCallee)
            case EP(_, GlobalEscape)              ⇒ calcMostRestrictive(GlobalEscape)
            case EP(_, EscapeViaStaticField)      ⇒ calcMostRestrictive(EscapeViaStaticField)
            case EP(_, EscapeViaHeapObject)       ⇒ calcMostRestrictive(EscapeViaHeapObject)
            case EP(_, EscapeViaReturn) ⇒
                assignment match {
                    case Some(_) ⇒
                        calcMostRestrictive(AtMost(EscapeInCallee))
                    case None ⇒
                        calcMostRestrictive(EscapeInCallee)
                }
            // we do not track parameters or exceptions in the callee side
            case EP(_, p) if p.isFinal ⇒ calcMostRestrictive(AtMost(EscapeInCallee))
            case EP(_, AtMost(_))      ⇒ calcMostRestrictive(AtMost(EscapeInCallee))
            case epk ⇒
                assert(epk.e.isInstanceOf[FormalParameter] || epk.e.isInstanceOf[VirtualFormalParameter])
                dependees += epk
                dependeeToStmt += ((fp, assignment))
                calcMostRestrictive(EscapeInCallee)
        }
    }

    abstract override protected[this] def c(
        other: Entity, p: Property, u: UpdateType
    ): PropertyComputationResult = {
        other match {
            // this entity is passed as parameter (or this local) to a method
            case _: FormalParameter | _: VirtualFormalParameter ⇒ p match {

                case GlobalEscape         ⇒ Result(e, GlobalEscape)

                case EscapeViaStaticField ⇒ Result(e, EscapeViaStaticField)

                case EscapeViaHeapObject  ⇒ Result(e, EscapeViaHeapObject)

                case NoEscape | EscapeInCallee ⇒
                    removeFromDependeesAndComputeResult(other, EscapeInCallee)

                case EscapeViaParameter ⇒
                    // IMPROVE we do not further track the field of the actual parameter
                    removeFromDependeesAndComputeResult(other, AtMost(EscapeInCallee))

                case EscapeViaAbnormalReturn ⇒
                    // IMPROVE we do not further track the exception thrown in the callee
                    removeFromDependeesAndComputeResult(other, AtMost(EscapeInCallee))

                case EscapeViaReturn ⇒
                    /*
                     * IMPROVE we do not further track the return value of the callee.
                     * But the org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
                     * eliminates the assignments, if the function called is identity-like
                     */
                    val assignment = dependeeToStmt(other)
                    assignment match {
                        case Some(_) ⇒
                            removeFromDependeesAndComputeResult(other, AtMost(EscapeInCallee))
                        case None ⇒
                            removeFromDependeesAndComputeResult(other, EscapeInCallee)
                    }

                case EscapeViaParameterAndAbnormalReturn | EscapeViaNormalAndAbnormalReturn |
                    EscapeViaParameterAndAbnormalReturn | EscapeViaParameterAndReturn |
                    EscapeViaParameterAndNormalAndAbnormalReturn ⇒
                    // combines the cases above
                    removeFromDependeesAndComputeResult(other, AtMost(EscapeInCallee))

                case AtMost(_) ⇒
                    removeFromDependeesAndComputeResult(other, AtMost(EscapeInCallee))

                case Conditional(NoEscape) | Conditional(EscapeInCallee) ⇒
                    performIntermediateUpdate(other, p.asInstanceOf[EscapeProperty], EscapeInCallee)

                case p @ Conditional(EscapeViaReturn) ⇒
                    val assignment = dependeeToStmt(other)
                    assignment match {
                        case Some(_) ⇒
                            performIntermediateUpdate(other, p, AtMost(EscapeInCallee))
                        case None ⇒
                            performIntermediateUpdate(other, p, EscapeInCallee)
                    }

                case p @ Conditional(_) ⇒
                    performIntermediateUpdate(other, p, AtMost(EscapeInCallee))

                case _ ⇒
                    throw new UnknownError(s"unexpected escape property ($p) for $other")
            }
            case _ ⇒ super.c(other, p, u)
        }
    }
}

class InterProceduralEntityEscapeAnalysis(
    val e:                       Entity,
    val defSite:                 ValueOrigin,
    val uses:                    IntTrieSet,
    val code:                    Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]],
    val params:                  Parameters[TACMethodParameter],
    val cfg:                     CFG,
    val handlers:                ExceptionHandlers,
    val aiResult:                AIResult,
    val formalParameters:        FormalParameters,
    val virtualFormalParameters: VirtualFormalParameters,
    val m:                       VirtualMethod,
    val propertyStore:           PropertyStore,
    val project:                 SomeProject
) extends DefaultEntityEscapeAnalysis
        with ConstructorSensitiveEntityEscapeAnalysis
        with ConfigurationBasedConstructorEscapeAnalysis
        with SimpleFieldAwareEntityEscapeAnalysis
        with ExceptionAwareEntityEscapeAnalysis
        with AbstractInterProceduralEntityEscapeAnalysis
