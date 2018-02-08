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
import org.opalj.ai.ValueOrigin
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ObjectType
import org.opalj.br.AllocationSite
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeViaStaticField
import org.opalj.fpcf.properties.EscapeViaHeapObject
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.Conditional
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.ArrayStore
import org.opalj.tac.PutField
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
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
    val entity:                  Entity,
    val defSite:                 ValueOrigin,
    val uses:                    IntTrieSet,
    val code:                    Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]],
    val cfg:                     CFG,
    val declaredMethods:         DeclaredMethods,
    val virtualFormalParameters: VirtualFormalParameters,
    val propertyStore:           PropertyStore,
    val project:                 SomeProject
) extends DefaultEntityEscapeAnalysis
        with ConstructorSensitiveEntityEscapeAnalysis
        with ConfigurationBasedConstructorEscapeAnalysis
        with SimpleFieldAwareEntityEscapeAnalysis
        with ExceptionAwareEntityEscapeAnalysis

/**
 * Handling for exceptions, that are allocated within the current method.
 * Only if the throw stmt leads to an abnormal return in the cfg, the escape state is decreased
 * down to [[org.opalj.fpcf.properties.EscapeViaAbnormalReturn]]. Otherwise (the exception is caught)
 * the escape state remains the same.
 */
trait ExceptionAwareEntityEscapeAnalysis extends AbstractEntityEscapeAnalysis {

    val cfg: CFG
    val project: SomeProject

    override protected[this] def handleThrow(aThrow: Throw[V]): Unit = {
        if (usesDefSite(aThrow.exception)) {
            val index = code indexWhere {
                _ == aThrow
            }
            val successors = cfg.bb(index).successors

            var isCaught = false
            var abnormalReturned = false
            for (pc ← successors) {
                if (pc.isCatchNode) {
                    val exceptionType = entity match {
                        case as: AllocationSite ⇒ as.allocatedType
                        case VirtualFormalParameter(DefinedMethod(_, callee), -1) ⇒
                            callee.classFile.thisType
                        case VirtualFormalParameter(callee, origin) ⇒
                            // we would not end in this case if the parameter is not an object
                            callee.descriptor.parameterTypes(-2 - origin).asObjectType
                    }
                    pc.asCatchNode.catchType match {
                        case Some(catchType) ⇒
                            if (project.classHierarchy.isSubtypeOf(exceptionType, catchType).isYes)
                                isCaught = true
                        case None ⇒
                    }
                } else if (pc.isAbnormalReturnExitNode) {
                    abnormalReturned = true
                }
            }
            if (abnormalReturned && !isCaught) {
                meetMostRestrictive(EscapeViaAbnormalReturn)
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

    override protected[this] def handlePutField(putField: PutField[V]): Unit = {
        if (usesDefSite(putField.value))
            handleFieldLike(putField.objRef.asVar.definedBy)
    }

    override protected[this] def handleArrayStore(arrayStore: ArrayStore[V]): Unit = {
        if (usesDefSite(arrayStore.value))
            handleFieldLike(arrayStore.arrayRef.asVar.definedBy)
    }

    /**
     * A worklist algorithm, check the def sites of the reference of the field, or array, to which
     * the current entity was assigned.
     */
    private[this] def handleFieldLike(referenceDefSites: IntTrieSet): Unit = {
        // the definition sites to handle
        var workset = referenceDefSites

        // the definition sites that were already handled
        var seen: IntTrieSet = EmptyIntTrieSet

        while (workset.nonEmpty) {
            val (referenceDefSite, newWorklist) = workset.getAndRemove
            workset = newWorklist
            seen += referenceDefSite

            // do not check the escape state of the entity (defSite) whose escape state we are
            // currently computing to avoid endless loops
            if (defSite != referenceDefSite) {
                // is the object/array reference of the field a local
                if (referenceDefSite >= 0) {
                    code(referenceDefSite) match {
                        case Assignment(_, _, New(_, _) | NewArray(_, _, _)) ⇒
                            /* as may alias information are not easily available we cannot simply
                            check for escape information of the base object */
                            meetMostRestrictive(AtMost(NoEscape))
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
                            meetMostRestrictive(EscapeViaHeapObject)
                        case Assignment(_, _, GetField(_, _, _, _, objRef)) ⇒
                            objRef.asVar.definedBy foreach { x ⇒
                                if (!seen.contains(x))
                                    workset += x
                            }
                        case Assignment(_, _, ArrayLoad(_, _, arrayRef)) ⇒
                            arrayRef.asVar.definedBy foreach { x ⇒
                                if (!seen.contains(x))
                                    workset += x
                            }
                        // we are not inter-procedural
                        case Assignment(_, _, _: FunctionCall[_]) ⇒ meetMostRestrictive(AtMost(NoEscape))
                        case Assignment(_, _, _: Const)           ⇒
                        case s                                    ⇒ throw new UnknownError(s"Unexpected tac: $s")
                    }

                } else if (referenceDefSite >= ai.VMLevelValuesOriginOffset) {
                    // assigned to field of parameter
                    meetMostRestrictive(AtMost(EscapeViaParameter))
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
                } else throw new UnknownError(s"Unexpected origin $referenceDefSite")
            }
        }
    }
}

/**
 * In the configuration system it is possible to define escape information for the this local in the
 * constructors of a specific class. This analysis sets the [[org.opalj.br.analyses.VirtualFormalParameter]] of the this local
 * to the defined value.
 */
trait ConfigurationBasedConstructorEscapeAnalysis extends AbstractEntityEscapeAnalysis {
    protected[this] abstract override def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    ): Unit = {
        assert(call.name == "<init>")
        assert(usesDefSite(call.receiver))
        assert(call.declaringClass.isObjectType)

        val propertyOption = ConfigurationBasedConstructorEscapeAnalysis.constructors.get(
            call.declaringClass.asObjectType
        )

        // the object constructor will not escape the this local
        if (propertyOption.nonEmpty) {
            meetMostRestrictive(propertyOption.get)
        } else {
            super.handleThisLocalOfConstructor(call)
        }
    }
}

/**
 * The companion object of the [[ConfigurationBasedConstructorEscapeAnalysis]] that statically
 * loads the configuration and gets the escape property objects via reflection.
 *
 * @note The reflective code assumes that every [[EscapeProperty]] is an object and not a class.
 */
object ConfigurationBasedConstructorEscapeAnalysis {

    private[this] case class PredefinedResult(object_type: String, escape_of_this: String)

    import net.ceedubs.ficus.Ficus._
    import net.ceedubs.ficus.readers.ArbitraryTypeReader._

    val ConfigKey = "org.opalj.fpcf.analyses.ConfigurationBasedConstructorEscapeAnalysis.constructors"
    val constructors: Map[ObjectType, EscapeProperty] =
        BaseConfig.as[Seq[PredefinedResult]](ConfigKey).map { r ⇒
            import scala.reflect.runtime._
            val rootMirror = universe.runtimeMirror(getClass.getClassLoader)
            val module = rootMirror.staticModule(r.escape_of_this)
            val property = rootMirror.reflectModule(module).instance.asInstanceOf[EscapeProperty]
            (ObjectType(r.object_type), property)
        }.toMap
}

/**
 * Special handling for constructor calls, as the receiver of an constructor is always an
 * allocation site.
 * The constructor of Object does not escape the self reference by definition. For other
 * constructors, the inter-procedural chain will be processed until it reaches the Object
 * constructor or escapes. Is this the case, leastRestrictiveProperty will be set to the lower bound
 * of the current value and the calculated escape state.
 *
 * For non constructor calls, [[org.opalj.fpcf.properties.AtMost(EscapeInCallee)]] of `e will be `
 * returned whenever the receiver or a parameter is a use of defSite.
 */
trait ConstructorSensitiveEntityEscapeAnalysis extends AbstractEntityEscapeAnalysis {
    val project: SomeProject
    val propertyStore: PropertyStore
    val virtualFormalParameters: VirtualFormalParameters
    val declaredMethods: DeclaredMethods

    abstract protected[this] override def handleThisLocalOfConstructor(call: NonVirtualMethodCall[V]): Unit = {
        assert(call.name == "<init>")
        assert(usesDefSite(call.receiver))

        // the object constructor will not escape the this local
        if (call.declaringClass eq ObjectType.Object)
            return ;

        // resolve the constructor
        project.specialCall(
            call.declaringClass,
            call.isInterface,
            "<init>",
            call.descriptor
        ) match {
                case Success(callee) ⇒
                    // check if the this local escapes in the callee
                    val escapeState = propertyStore(virtualFormalParameters(declaredMethods(callee))(0), EscapeProperty.key)
                    escapeState match {
                        case EP(_, NoEscape)                                    ⇒ //NOTHING TO DO
                        case EP(_, GlobalEscape)                                ⇒ meetMostRestrictive(GlobalEscape)
                        case EP(_, EscapeViaStaticField)                        ⇒ meetMostRestrictive(EscapeViaStaticField)
                        case EP(_, EscapeViaHeapObject)                         ⇒ meetMostRestrictive(EscapeViaHeapObject)
                        case EP(_, EscapeInCallee)                              ⇒ meetMostRestrictive(EscapeInCallee)
                        case EP(_, AtMost(EscapeInCallee))                      ⇒ meetMostRestrictive(AtMost(EscapeInCallee))
                        case EP(_, EscapeViaParameter)                          ⇒ meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, EscapeViaAbnormalReturn)                     ⇒ meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, EscapeViaParameterAndAbnormalReturn)         ⇒ meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, AtMost(NoEscape))                            ⇒ meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, AtMost(EscapeViaParameter))                  ⇒ meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, AtMost(EscapeViaAbnormalReturn))             ⇒ meetMostRestrictive(AtMost(NoEscape))
                        case EP(_, AtMost(EscapeViaParameterAndAbnormalReturn)) ⇒ meetMostRestrictive(AtMost(NoEscape))
                        case ep @ EP(_, Conditional(NoEscape)) ⇒
                            dependees += ep
                        case ep @ EP(_, Conditional(EscapeInCallee)) ⇒
                            meetMostRestrictive(EscapeInCallee)
                            dependees += ep
                        case ep @ EP(_, Conditional(AtMost(EscapeInCallee))) ⇒
                            meetMostRestrictive(AtMost(EscapeInCallee))
                            dependees += ep
                        case ep @ EP(_, Conditional(_)) ⇒
                            meetMostRestrictive(AtMost(NoEscape))
                            dependees += ep
                        case EP(_, p) ⇒
                            throw new UnknownError(s"unexpected escape property ($p) for constructors")
                        // result not yet finished
                        case epk ⇒
                            dependees += epk
                    }
                case /* unknown method */ _ ⇒ meetMostRestrictive(AtMost(NoEscape))
            }
    }

    abstract override protected[this] def c(
        other: Entity, p: Property, u: UpdateType
    ): PropertyComputationResult = {
        other match {
            case VirtualFormalParameter(DefinedMethod(_, method), -1) if method.isConstructor ⇒ p match {

                case GlobalEscape         ⇒ Result(entity, GlobalEscape)

                case EscapeViaStaticField ⇒ Result(entity, EscapeViaStaticField)

                case EscapeViaHeapObject  ⇒ Result(entity, EscapeViaHeapObject)

                case NoEscape             ⇒ removeFromDependeesAndComputeResult(other, NoEscape)

                case EscapeInCallee       ⇒ removeFromDependeesAndComputeResult(other, EscapeInCallee)

                case EscapeViaParameter ⇒
                    // we do not further track the field of the actual parameter
                    removeFromDependeesAndComputeResult(other, AtMost(NoEscape))

                case EscapeViaAbnormalReturn ⇒
                    // this could be the case if `other` is an exception and is thrown in its constructor
                    removeFromDependeesAndComputeResult(other, AtMost(NoEscape))

                case EscapeViaParameterAndAbnormalReturn ⇒
                    // combines the two cases above
                    removeFromDependeesAndComputeResult(other, AtMost(NoEscape))

                case AtMost(NoEscape) | AtMost(EscapeViaParameter) | AtMost(EscapeViaAbnormalReturn) |
                    AtMost(EscapeViaParameterAndAbnormalReturn) ⇒
                    //assert(u ne IntermediateUpdate)
                    removeFromDependeesAndComputeResult(other, AtMost(NoEscape))

                case AtMost(EscapeInCallee) ⇒
                    //assert(u ne IntermediateUpdate)
                    removeFromDependeesAndComputeResult(other, AtMost(EscapeInCallee))

                case p @ Conditional(NoEscape) ⇒
                    assert(u eq IntermediateUpdate)
                    performIntermediateUpdate(other, p, NoEscape)

                case p @ Conditional(EscapeInCallee) ⇒
                    assert(u eq IntermediateUpdate)
                    performIntermediateUpdate(other, p, EscapeInCallee)

                case p @ Conditional(AtMost(EscapeInCallee)) ⇒
                    assert(u eq IntermediateUpdate)
                    performIntermediateUpdate(other, p, AtMost(EscapeInCallee))

                case p @ Conditional(_) ⇒
                    assert(u eq IntermediateUpdate)
                    performIntermediateUpdate(other, p, AtMost(NoEscape))

                case _ ⇒
                    throw new UnknownError(s"unexpected escape property ($p) for constructors")
            }
            case _ ⇒ super.c(other, p, u)
        }
    }
}
