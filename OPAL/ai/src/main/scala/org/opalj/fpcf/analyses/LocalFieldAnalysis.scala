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

import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.analyses.escape.DefaultEntityEscapeAnalysis
import org.opalj.fpcf.properties.ConditionalFreshReturnValue
import org.opalj.fpcf.properties.ConditionalLocalField
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.LocalField
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.NoLocalField
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VConditionalFreshReturnValue
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VPrimitiveReturnValue
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.tac.Assignment
import org.opalj.tac.Const
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.PutField
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall

class LocalFieldAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]
    private[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)
    private[this] val declaredMethods: DeclaredMethods = propertyStore.context[DeclaredMethods]

    def determineLocality(field: Field): PropertyComputationResult = {
        // base types can be considered to be local
        if (field.fieldType.isBaseType)
            return Result(field, LocalField)

        // this analysis can only track private fields
        if (!field.isPrivate)
            return Result(field, NoLocalField)

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        val methods = field.classFile.methodsWithBody.map(_._1)

        // all assignments to the field have to be fresh
        for {
            method ← methods
            stmts = tacaiProvider(method).stmts
            stmt ← stmts
        } {
            stmt match {
                case PutField(_, dc, name, fieldType, objRef, value) if dc == field.classFile.thisType && name == field.name && fieldType == field.fieldType ⇒
                    if (objRef.asVar.definedBy != IntTrieSet(-1))
                        return Result(field, NoLocalField)
                    for (defSite1 ← value.asVar.definedBy) {
                        if (defSite1 < 0)
                            return Result(field, NoLocalField)

                        val uses1 = stmts(defSite1) match {
                            case Assignment(_, tgt, _) ⇒ tgt.usedBy
                            case _                     ⇒ throw new Error("unexpected def-Site")
                        }

                        // the reference stored in the field does not escape by other means
                        new DefaultEntityEscapeAnalysis {
                            override val code: Array[Stmt[V]] = stmts
                            override val defSite: ValueOrigin = defSite1
                            override val uses: IntTrieSet = uses1.filter(_ != stmts.indexOf(stmt))
                            override val entity: Entity = null
                        }.doDetermineEscape() match {
                            case Result(_, NoEscape) ⇒
                            case _                   ⇒ return Result(field, NoLocalField)
                        }

                        def handleConcreteCall(callee: org.opalj.Result[Method]): Option[PropertyComputationResult] = {
                            // unkown method
                            if (callee.isEmpty)
                                return Some(Result(field, NoLocalField))

                            propertyStore(declaredMethods(callee.value), ReturnValueFreshness.key) match {
                                case EP(_, NoFreshReturnValue)   ⇒ return Some(Result(field, NoLocalField))
                                case EP(_, FreshReturnValue)     ⇒
                                case EP(_, PrimitiveReturnValue) ⇒
                                case epkOrCond                   ⇒ dependees += epkOrCond
                            }
                            None
                        }

                        // the object stored in the field is fresh
                        stmts(defSite1) match {
                            case Assignment(_, _, New(_, _) | NewArray(_, _, _)) ⇒ // fresh by definition

                            case Assignment(_, _, StaticFunctionCall(_, dc, isI, name, desc, _)) ⇒
                                val callee = project.staticCall(dc, isI, name, desc)
                                handleConcreteCall(callee).foreach(return _)

                            case Assignment(_, _, NonVirtualFunctionCall(_, dc, isI, name, desc, _, _)) ⇒
                                val callee = project.specialCall(dc, isI, name, desc)
                                handleConcreteCall(callee).foreach(return _)

                            case Assignment(_, _, VirtualFunctionCall(_, dc, _, name, desc, receiver, _)) ⇒

                                val value = receiver.asVar.value.asDomainReferenceValue
                                if (dc.isArrayType) {
                                    val callee = project.instanceCall(ObjectType.Object, ObjectType.Object, name, desc)
                                    handleConcreteCall(callee).foreach(return _)
                                } else if (value.isPrecise) {
                                    val preciseType = value.valueType.get
                                    val callee = project.instanceCall(method.classFile.thisType, preciseType, name, desc)
                                    handleConcreteCall(callee).map(return _)
                                } else {
                                    val callee = project.instanceCall(method.classFile.thisType, dc, name, desc)
                                    if (callee.isEmpty)
                                        return Result(field, NoLocalField)

                                    propertyStore(declaredMethods(callee.value), VirtualMethodReturnValueFreshness.key) match {
                                        case EP(_, VNoFreshReturnValue) ⇒
                                            return Result(field, NoLocalField)
                                        case EP(_, VFreshReturnValue)     ⇒
                                        case EP(_, VPrimitiveReturnValue) ⇒
                                        case epkOrCnd ⇒
                                            dependees += epkOrCnd
                                    }
                                }
                            case Assignment(_, _, _: Const) ⇒

                            case Assignment(_, _, GetField(_, _, _, _, _)) ⇒ //TODO is local?
                                return Result(field, NoLocalField)

                            case Assignment(_, _, GetStatic(_, _, _, _)) ⇒
                                return Result(field, NoLocalField)

                            case _ ⇒
                                return Result(field, NoLocalField)

                        }
                    }

                case _ ⇒
            }

        }

        // no read from field escapes
        for {
            method ← methods
            stmts = tacaiProvider(method).stmts
            stmt ← stmts
        } {
            stmt match {
                case Assignment(_, tgt, GetField(_, dc, name, fieldType, objRef)) if dc == field.classFile.thisType && field.name == name && fieldType == field.fieldType ⇒
                    if (objRef.asVar.definedBy != IntTrieSet(-1))
                        return Result(field, NoLocalField)

                    new DefaultEntityEscapeAnalysis {
                        override val code: Array[Stmt[V]] = stmts
                        override val defSite: ValueOrigin = stmts.indexOf(stmt)
                        override val uses: IntTrieSet = tgt.usedBy
                        override val entity: Entity = null
                    }.doDetermineEscape() match {
                        case Result(_, NoEscape) ⇒
                        case _                   ⇒ return Result(field, NoLocalField)
                    }
                case _ ⇒
            }
        }

        def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
            p match {
                case NoFreshReturnValue | VNoFreshReturnValue ⇒
                    Result(field, NoLocalField)

                case FreshReturnValue | VFreshReturnValue | PrimitiveReturnValue | VPrimitiveReturnValue ⇒
                    dependees = dependees.filter(epk ⇒ (epk.e ne e) || epk.pk != p.key)
                    if (dependees.isEmpty)
                        Result(field, LocalField)
                    else
                        IntermediateResult(field, ConditionalLocalField, dependees, c)
                case ConditionalFreshReturnValue | VConditionalFreshReturnValue ⇒
                    val newEP = EP(e, p)
                    dependees = dependees.filter(epk ⇒ (epk.e ne e) || epk.pk != p.key) + newEP
                    IntermediateResult(field, ConditionalLocalField, dependees, c)

                case PropertyIsLazilyComputed ⇒
                    IntermediateResult(field, ConditionalLocalField, dependees, c)
            }
        }

        if (dependees.isEmpty)
            Result(field, LocalField)
        else
            IntermediateResult(field, ConditionalLocalField, dependees, c)
    }
}

object LocalFieldAnalysis extends FPCFAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val allFields = project.allFields
        val analysis = new LocalFieldAnalysis(project)
        propertyStore.scheduleForEntities(allFields)(analysis.determineLocality)
        analysis
    }

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new LocalFieldAnalysis(project)
        propertyStore.scheduleLazyPropertyComputation(
            FieldLocality.key, analysis.determineLocality
        )
        analysis
    }

    override def derivedProperties: Set[PropertyKind] = Set(FieldLocality)

    override def usedProperties: Set[PropertyKind] =
        Set(ReturnValueFreshness, VirtualMethodReturnValueFreshness)

}
