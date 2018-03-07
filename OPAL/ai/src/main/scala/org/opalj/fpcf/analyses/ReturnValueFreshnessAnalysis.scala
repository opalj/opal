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

import org.opalj
import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.AllocationSite
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.AllocationSites
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.analyses.escape.AbstractEscapeAnalysisState
import org.opalj.fpcf.analyses.escape.FallBackEscapeAnalysis
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.ConditionalFreshReturnValue
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VConditionalFreshReturnValue
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.tac.Assignment
import org.opalj.tac.Const
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall

/**
 * An analysis that determines for a given method, whether its the return value is a fresh object,
 * that is created within the method and does not escape by other than [[EscapeViaReturn]].
 *
 * In other words, it aggregates the escape information for all allocation-sites, that might be used
 * as return value.
 *
 * @author Florian Kuebler
 */
class ReturnValueFreshnessAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]
    private[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)
    private[this] val allocationSites: AllocationSites = propertyStore.context[AllocationSites]
    private[this] val declaredMethods: DeclaredMethods = propertyStore.context[DeclaredMethods]

    def doDetermineFreshness(dm: DefinedMethod): PropertyComputationResult = {
        if (dm.descriptor.returnType.isBaseType)
            return Result(dm, PrimitiveReturnValue)
        if (!dm.hasDefinition)
            return Result(dm, NoFreshReturnValue)

        if (dm.declaringClassType.isArrayType) {
            if (dm.name == "clone" && dm.descriptor == MethodDescriptor.JustReturnsObject) {
                return Result(dm, FreshReturnValue)
            }
        }

        val m = dm.definedMethod

        if (m.body.isEmpty)
            return Result(dm, NoFreshReturnValue)

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty
        val code = tacaiProvider(m).stmts

        // for every return-value statement check the def-sites
        for {
            ReturnValue(_, expr) ← code
            defSite ← expr.asVar.definedBy
        } {
            // parameters are not fresh by definition
            if (defSite < 0)
                return Result(dm, NoFreshReturnValue)

            code(defSite) match {
                // if the def-site of the return-value statement is a new, we check the escape state
                case Assignment(pc, _, New(_, _) | NewArray(_, _, _)) ⇒
                    val allocationSite = allocationSites(m)(pc)
                    val resultState = propertyStore(allocationSite, EscapeProperty.key)
                    resultState match {
                        case EP(_, EscapeViaReturn)              ⇒
                        case EP(_, NoEscape | EscapeInCallee)    ⇒ throw new RuntimeException("unexpected result")
                        case EP(_, p) if p.isFinal               ⇒ return Result(dm, NoFreshReturnValue)
                        case EP(_, AtMost(_))                    ⇒ return Result(dm, NoFreshReturnValue)
                        case EP(_, Conditional(AtMost(_)))       ⇒ return Result(dm, NoFreshReturnValue)
                        case EP(_, Conditional(NoEscape))        ⇒ throw new RuntimeException("unexpected result")
                        case EP(_, Conditional(EscapeInCallee))  ⇒ throw new RuntimeException("unexpected result")
                        case EP(_, Conditional(EscapeViaReturn)) ⇒ dependees += resultState
                        case EP(_, Conditional(_))               ⇒ return Result(dm, NoFreshReturnValue)
                        case _                                   ⇒ dependees += resultState
                    }

                // const values are handled as fresh
                case Assignment(_, _, _: Const) ⇒

                case Assignment(_, tgt, StaticFunctionCall(_, dc, isI, name, desc, _)) ⇒
                    handleEscape(defSite, tgt.usedBy, code).foreach(return _)

                    val callee = project.staticCall(dc, isI, name, desc)
                    handleConcreteCall(callee).foreach(return _)

                case Assignment(_, tgt, NonVirtualFunctionCall(_, dc, isI, name, desc, _, _)) ⇒
                    handleEscape(defSite, tgt.usedBy, code).foreach(return _)

                    val callee = project.specialCall(dc, isI, name, desc)
                    handleConcreteCall(callee).foreach(return _)

                case Assignment(_, tgt, VirtualFunctionCall(_, dc, _, name, desc, receiver, _)) ⇒
                    handleEscape(defSite, tgt.usedBy, code).foreach(return _)

                    val value = receiver.asVar.value.asDomainReferenceValue

                    if (value.isNull.isNotYes) {
                        val receiverType = project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                            value.upperTypeBound
                        )

                        if (receiverType.isArrayType) {
                            val callee = project.instanceCall(ObjectType.Object, ObjectType.Object, name, desc)
                            handleConcreteCall(callee).foreach(return _)
                        } else if (value.isPrecise) {
                            val preciseType = value.valueType.get
                            val callee = project.instanceCall(m.classFile.thisType, preciseType, name, desc)
                            handleConcreteCall(callee).foreach(return _)
                        } else {
                            var callee = project.instanceCall(m.classFile.thisType, dc, name, desc)

                            // check if the method is abstract?
                            if (callee.isEmpty) {
                                project.classFile(receiverType.asObjectType) match {
                                    case Some(cf) ⇒
                                        callee = if (cf.isInterfaceDeclaration) {
                                            org.opalj.Result(
                                                project.resolveInterfaceMethodReference(receiverType.asObjectType, name, desc)
                                            )
                                        } else {
                                            project.resolveClassMethodReference(receiverType.asObjectType, name, desc)
                                        }
                                    case None ⇒
                                        return Result(dm, NoFreshReturnValue)
                                }
                            }

                            // unkown method
                            if (callee.isEmpty)
                                return Result(dm, NoFreshReturnValue)

                            propertyStore(declaredMethods(callee.value), VirtualMethodReturnValueFreshness.key) match {
                                case EP(_, VNoFreshReturnValue) ⇒
                                    return Result(dm, NoFreshReturnValue)
                                case EP(_, VFreshReturnValue) ⇒
                                case epkOrCond                ⇒ dependees += epkOrCond
                            }
                        }
                    }
                // other kinds of assignments came from other methods, fields etc, which we do not track
                case Assignment(_, _, _) ⇒ return Result(dm, NoFreshReturnValue)
                case _                   ⇒ throw new RuntimeException("not yet implemented")
            }
        }

        def handleConcreteCall(callee: opalj.Result[Method]): Option[PropertyComputationResult] = {
            // unkown method
            if (callee.isEmpty)
                return Some(Result(dm, NoFreshReturnValue))

            propertyStore(declaredMethods(callee.value), ReturnValueFreshness.key) match {
                case EP(_, NoFreshReturnValue) ⇒
                    return Some(Result(dm, NoFreshReturnValue))
                case EP(_, FreshReturnValue) ⇒
                case epkOrCond               ⇒ dependees += epkOrCond
            }
            None
        }

        def handleEscape(defSite: ValueOrigin, uses: IntTrieSet, code: Array[Stmt[V]]) = {
            val analysis = new FallBackEscapeAnalysis(project)
            val ctx = analysis.createContext(null, defSite, null, uses, code, null)
            val escapeState = new AbstractEscapeAnalysisState {}

            analysis.doDetermineEscape(ctx, escapeState) match {
                case Result(_, EscapeViaReturn) ⇒ None
                case _                          ⇒ Some(Result(dm, NoFreshReturnValue))
            }
        }

        /**
         * A continuation function, that handles updates for the escape state.
         */
        def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
            e match {

                case _: AllocationSite ⇒ p match {
                    case NoEscape | EscapeInCallee ⇒
                        throw new RuntimeException("unexpected result")

                    case EscapeViaReturn ⇒
                        dependees = dependees.filter(epk ⇒ (epk.e ne e) || epk.pk != p.key)
                        if (dependees.isEmpty) {
                            Result(dm, FreshReturnValue)
                        } else {
                            IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)
                        }

                    //TODO what if escapes via exceptions?
                    case _: EscapeProperty if p.isFinal ⇒ Result(dm, NoFreshReturnValue)

                    // it could happen anything
                    case AtMost(_)                      ⇒ Result(dm, NoFreshReturnValue)
                    case Conditional(AtMost(_))         ⇒ Result(dm, NoFreshReturnValue)

                    case p @ Conditional(EscapeViaReturn) ⇒
                        val newEP = EP(e, p)
                        dependees = dependees.filter(epk ⇒ (epk.e ne e) || epk.pk != p.key) + newEP
                        IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)

                    case Conditional(NoEscape) | Conditional(EscapeInCallee) ⇒
                        throw new RuntimeException("unexpected result")

                    // p is worse than via return
                    case Conditional(_) ⇒ Result(dm, NoFreshReturnValue)

                    case PropertyIsLazilyComputed ⇒
                        IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)

                }

                case _: DeclaredMethod ⇒ p match {
                    case NoFreshReturnValue | VNoFreshReturnValue ⇒
                        Result(dm, NoFreshReturnValue)
                    case FreshReturnValue | VFreshReturnValue ⇒
                        dependees = dependees.filter(epk ⇒ (epk.e ne e) || epk.pk != p.key)
                        if (dependees.isEmpty)
                            Result(dm, FreshReturnValue)
                        else
                            IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)

                    case ConditionalFreshReturnValue | VConditionalFreshReturnValue ⇒
                        val newEP = EP(e, p)
                        dependees = dependees.filter(epk ⇒ (epk.e ne e) || epk.pk != p.key) + newEP
                        IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)

                    case PropertyIsLazilyComputed ⇒
                        IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)
                }
            }

        }

        if (dependees.isEmpty) {
            Result(dm, FreshReturnValue)
        } else {
            IntermediateResult(dm, ConditionalFreshReturnValue, dependees, c)
        }
    }

    /**
     * Determines the freshness of the return value.
     */
    def determineFreshness(m: DeclaredMethod): PropertyComputationResult = {
        m match {
            case dm @ DefinedMethod(_, me) if me.body.isDefined ⇒ doDetermineFreshness(dm)
            case dm @ DefinedMethod(_, me) if me.body.isEmpty   ⇒ Result(dm, NoFreshReturnValue)
            case _                                              ⇒ throw new NotImplementedError()
        }
    }
}

object ReturnValueFreshnessAnalysis extends FPCFAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(ReturnValueFreshness)

    override def usedProperties: Set[PropertyKind] =
        Set(EscapeProperty, VirtualMethodReturnValueFreshness)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val declaredMethods = propertyStore.context[DeclaredMethods].declaredMethods
        val analysis = new ReturnValueFreshnessAnalysis(project)
        propertyStore.scheduleForEntities(declaredMethods)(analysis.determineFreshness)
        analysis
    }

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new ReturnValueFreshnessAnalysis(project)
        propertyStore.scheduleLazyPropertyComputation(
            ReturnValueFreshness.key, analysis.doDetermineFreshness
        )
        analysis
    }
}
