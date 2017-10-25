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

import org.opalj.ai.Domain
import org.opalj.ai.AIResult
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.ExceptionHandlers
import org.opalj.br.AllocationSite
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntArraySet
import org.opalj.collection.immutable.EmptyIntArraySet
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.MaybeEscapeViaParameter
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.MaybeEscapeInCallee
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.MaybeEscapeViaAbnormalReturn
import org.opalj.fpcf.properties.MaybeEscapeViaParameterAndAbnormalReturn
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.ArrayStore
import org.opalj.tac.PutField
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.Parameters
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.DUVar
import org.opalj.tac.GetField
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Const
import org.opalj.tac.GetStatic
import org.opalj.tac.New
import org.opalj.tac.FunctionCall
import org.opalj.tac.NewArray
import org.opalj.tac.Throw

/**
 * A very simple intra-procedural escape analysis, that has inter-procedural behavior for the `this`
 * local and has simple field handling.
 *
 * @author Florian Kuebler
 */
class SimpleEntityEscapeAnalysis(
    val e:             Entity,
    var defSite:       IntArraySet,
    val uses:          IntArraySet,
    val code:          Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]],
    val params:        Parameters[TACMethodParameter],
    val cfg:           CFG,
    val handlers:      ExceptionHandlers,
    val aiResult:      AIResult,
    val m:             Method,
    val propertyStore: PropertyStore,
    val project:       SomeProject
) extends AbstractEntityEscapeAnalysis
        with ConstructorSensitiveEntityEscapeAnalysis
        with SimpleFieldAwareEntityEscapeAnalysis
        with ExceptionAwareEntitiyEscapeAnalysis

/**
 * Handling for exceptions, that are allocated within the current method.
 * Only if the throw stmt leads to an abnormal return in the cfg, the escape state is decreased
 * down to [[org.opalj.fpcf.properties.EscapeViaAbnormalReturn]]. Otherwise (the exception is caught)
 * the escape state remains the same.
 */
trait ExceptionAwareEntitiyEscapeAnalysis extends AbstractEntityEscapeAnalysis {
    override protected def handleThrow(aThrow: Throw[V]): Unit = {
        if (usesDefSite(aThrow.exception)) {
            val index = code indexWhere { _ == aThrow }
            val successors = cfg.bb(index).successors

            var isCatched = false
            var abnormalReturned = false
            for (pc ← successors) {
                if (pc.isCatchNode) {
                    val exceptionType = e match {
                        case as: AllocationSite ⇒ as.allocatedType
                        case FormalParameter(m, -1) ⇒
                            m.classFile.thisType
                        case FormalParameter(m, origin) ⇒
                            // we would not end in this case if the parameter is not an object
                            m.parameterTypes(-1 - origin).asObjectType
                    }
                    pc.asCatchNode.catchType match {
                        case Some(catchType) ⇒
                            if (project.classHierarchy.isSubtypeOf(exceptionType, catchType).isYes)
                                isCatched = true
                        case None ⇒
                    }
                } else if (pc.isAbnormalReturnExitNode) {
                    abnormalReturned = true
                }
            }
            if (abnormalReturned && !isCatched) {
                calcMostRestrictive(EscapeViaAbnormalReturn)
            }
        }
    }
}

/**
 * Very simple handling for fields and arrays. This analysis can detect global escapes via
 * assignments to heap objects. Due to the lack of simple may-alias analysis, this analysis can not
 * determine [[org.opalj.fpcf.properties.NoEscape]] states.
 */
trait SimpleFieldAwareEntityEscapeAnalysis extends AbstractEntityEscapeAnalysis {

    override protected def handlePutField(putField: PutField[V]): Unit = {
        if (usesDefSite(putField.value))
            handleFieldLike(putField.objRef.asVar.definedBy)
    }

    override protected def handleArrayStore(arrayStore: ArrayStore[V]): Unit = {
        if (usesDefSite(arrayStore.value))
            handleFieldLike(arrayStore.arrayRef.asVar.definedBy)
    }

    /**
     * A worklist algorithm, check the def sites of the reference of the field, or array, to which
     * the current entity was assigned.
     */
    private def handleFieldLike(referenceDefSites: IntArraySet): Unit = {
        // the definition sites to handle
        var worklist = referenceDefSites

        // the definition sites that were already handled
        var seen: IntArraySet = EmptyIntArraySet

        while (worklist.nonEmpty) {
            val referenceDefSite = worklist.head
            worklist = worklist - referenceDefSite
            seen = seen + referenceDefSite

            // do not check the escape state of the entity (defSite) whose escape state we are
            // currently computing to avoid endless loops
            if (!defSite.contains(referenceDefSite)) {
                // is the object/array reference of the field a local
                if (referenceDefSite >= 0) {
                    code(referenceDefSite) match {
                        case Assignment(pc, _, New(_, _) | NewArray(_, _, _)) ⇒
                            /* as may alias information are not easily available we cannot simply
                            check for escape information of the base object */
                            calcMostRestrictive(MaybeNoEscape)
                        /*val allocationSites =
                                propertyStore.context[AllocationSites]
                            val allocationSite = allocationSites(m)(pc)
                            val escapeState = propertyStore(allocationSite, EscapeProperty.key)
                            escapeState match {
                                case EP(_, EscapeViaStaticField) ⇒ calcMostRestrictive(EscapeViaHeapObject)
                                case EP(_, EscapeViaHeapObject)  ⇒ calcMostRestrictive(EscapeViaStaticField)
                                case EP(_, GlobalEscape)         ⇒ calcMostRestrictive(GlobalEscape)
                                case EP(_, NoEscape)             ⇒ calcMostRestrictive(NoEscape)
                                case EP(_, p) if p.isFinal       ⇒ calcMostRestrictive(MaybeNoEscape)
                                case _                           ⇒ dependees += escapeState

                            }*/
                        /* if the base object came from a static field, the value assigned to it
                         escapes globally */
                        case Assignment(_, _, GetStatic(_, _, _, _)) ⇒
                            calcMostRestrictive(EscapeViaHeapObject)
                        case Assignment(_, _, GetField(_, _, _, _, objRef)) ⇒
                            objRef.asVar.definedBy foreach { x ⇒
                                if (!seen.contains(x)) worklist = worklist + x
                            }
                        case Assignment(_, _, ArrayLoad(_, _, arrayRef)) ⇒
                            arrayRef.asVar.definedBy foreach { x ⇒
                                if (!seen.contains(x)) worklist = worklist + x
                            }
                        // we are not inter-procedural
                        case Assignment(_, _, _: FunctionCall[_]) ⇒ calcMostRestrictive(MaybeNoEscape)
                        case Assignment(_, _, _: Const)           ⇒ // must be null
                        case _                                    ⇒ throw new RuntimeException("not yet implemented")
                    }

                } else if (referenceDefSite >= ai.VMLevelValuesOriginOffset) {
                    // assigned to field of parameter
                    calcMostRestrictive(MaybeEscapeViaParameter)
                    /* As may alias information are not easily available we cannot simply use
                    the code below:
                    val formalParameters = propertyStore.context[FormalParameters]
                    val formalParameter = formalParameters(m)(-referenceDefSite - 1)
                    val escapeState = propertyStore(formalParameter, EscapeProperty.key)
                    escapeState match {
                        case EP(_, p: GlobalEscape) ⇒ calcLeastRestrictive(p)
                        case EP(_, p) if p.isFinal  ⇒
                        case _                      ⇒ dependees += escapeState
                    }*/
                } else throw new RuntimeException("not yet implemented")
            }
        }
    }
}

/**
 * Simple inter-procedural handling for the `this` local of a constructor call.
 */
trait ConstructorSensitiveEntityEscapeAnalysis extends AbstractEntityEscapeAnalysis {

    /**
     * Special handling for constructor calls, as the receiver of an constructor is always an
     * allocation site.
     * The constructor of Object does not escape the self reference by definition. For other
     * constructors, the inter-procedural chain will be processed until it reaches the Object
     * constructor or escapes. Is this the case, leastRestrictiveProperty will be set to the lower bound
     * of the current value and the calculated escape state.
     *
     * For non constructor calls, [[org.opalj.fpcf.properties.MaybeEscapeInCallee]] of e will be returned
     * whenever the receiver or a parameter is a use of defSite.
     */
    override protected def handleNonVirtualMethodCall(call: NonVirtualMethodCall[V]): Unit = {
        // we only allow special (inter-procedural) handling for constructors
        if (call.name == "<init>") {
            if (usesDefSite(call.receiver)) {
                handleThisLocalOfConstructor(call)
            }
            handleParameterOfConstructor(call)
        } else {
            handleNonVirtualAndNonConstructorCall(call)
        }
    }

    protected def handleThisLocalOfConstructor(call: NonVirtualMethodCall[V]): Unit = {
        assert(call.name == "<init>")
        assert(usesDefSite(call.receiver))

        // the object constructor will not escape the this local
        if (call.declaringClass != ObjectType.Object && call.declaringClass != ObjectType.Throwable) {

            // resolve the constructor
            project.specialCall(
                call.declaringClass.asObjectType,
                call.isInterface,
                "<init>",
                call.descriptor
            ) match {
                    case Success(m) ⇒
                        val fp = propertyStore.context[FormalParameters]
                        // check if the this local escapes in the callee
                        val escapeState = propertyStore(fp(m)(0), EscapeProperty.key)
                        escapeState match {
                            case EP(_, NoEscape)                            ⇒ //NOTHING TO DO
                            case EP(_, GlobalEscape)                        ⇒ calcMostRestrictive(GlobalEscape)
                            case EP(_, EscapeViaStaticField)                ⇒ calcMostRestrictive(EscapeViaStaticField)
                            case EP(_, EscapeViaHeapObject)                 ⇒ calcMostRestrictive(EscapeViaHeapObject)
                            case EP(_, EscapeInCallee)                      ⇒ calcMostRestrictive(EscapeInCallee)
                            case EP(_, EscapeViaParameter)                  ⇒ calcMostRestrictive(MaybeNoEscape)
                            case EP(_, EscapeViaAbnormalReturn)             ⇒ calcMostRestrictive(MaybeNoEscape)
                            case EP(_, EscapeViaParameterAndAbnormalReturn) ⇒ calcMostRestrictive(MaybeNoEscape)
                            case EP(_, MaybeEscapeInCallee) ⇒
                                calcMostRestrictive(EscapeInCallee)
                                dependees += escapeState
                            case EP(_, MaybeNoEscape)                            ⇒ dependees += escapeState
                            case EP(_, MaybeEscapeViaParameter)                  ⇒ dependees += escapeState
                            case EP(_, MaybeEscapeViaAbnormalReturn)             ⇒ dependees += escapeState
                            case EP(_, MaybeEscapeViaParameterAndAbnormalReturn) ⇒ dependees += escapeState
                            case EP(_, p) ⇒
                                throw new UnknownError(s"unexpected escape property ($p) for constructors")
                            // result not yet finished
                            case epk ⇒ dependees += epk
                        }
                    case /* unknown method */ _ ⇒ calcMostRestrictive(MaybeNoEscape)
                }
        }
    }

    protected def handleParameterOfConstructor(call: NonVirtualMethodCall[V]): Unit = {
        if (anyParameterUsesDefSite(call.params))
            calcMostRestrictive(MaybeEscapeInCallee)
    }

    protected def handleNonVirtualAndNonConstructorCall(call: NonVirtualMethodCall[V]): Unit = {
        assert(call.name != "<init>")
        super.handleNonVirtualMethodCall(call)
    }
}
