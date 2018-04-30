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

import org.opalj.br.ArrayType
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions._
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.FinalField
import org.opalj.fpcf.properties.ImmutableContainerType
import org.opalj.fpcf.properties.ImmutableType
import org.opalj.fpcf.properties.Impure
import org.opalj.fpcf.properties.LBImpure
import org.opalj.fpcf.properties.NonFinalField
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.LBPure
import org.opalj.fpcf.properties.CompileTimePure

import scala.annotation.switch

/**
 * Very simple, fast, sound but also imprecise analysis of the purity of methods. See the
 * [[org.opalj.fpcf.properties.Purity]] property for details regarding the precise
 * semantics of `(Im)Pure`.
 *
 * This analysis is a very, very shallow implementation that immediately gives
 * up, when something "complicated" (e.g., method calls which take objects)
 * is encountered. It also does not perform any significant control-/data-flow analyses.
 *
 * @author Michael Eichberg
 */
class L0PurityAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {

    import project.nonVirtualCall
    import project.resolveFieldReference

    val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Determines the purity of the method starting with the instruction with the given
     * pc. If the given pc is larger than 0 then all previous instructions (in particular
     * method calls) must not violate this method's purity.
     *
     * This function encapsulates the continuation.
     */
    def doDeterminePurityOfBody(
        definedMethod:    DefinedMethod,
        initialDependees: Set[EOptionP[Entity, Property]]
    ): PropertyComputationResult = {

        val method = definedMethod.definedMethod
        val declaringClassType = method.classFile.thisType
        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.length

        var dependees = initialDependees

        var currentPC = 0
        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            (instruction.opcode: @switch) match {
                case GETSTATIC.opcode ⇒
                    val GETSTATIC(declaringClass, fieldName, fieldType) = instruction

                    resolveFieldReference(declaringClass, fieldName, fieldType) match {

                        // ... we have no support for arrays at the moment
                        case Some(field) if !field.fieldType.isArrayType ⇒
                            // The field has to be effectively final and -
                            // if it is an object – immutable!
                            val fieldType = field.fieldType
                            if (fieldType.isArrayType) {
                                return Result(declaringClass, LBImpure);
                            }
                            if (!fieldType.isBaseType) {
                                propertyStore(fieldType, TypeImmutability.key) match {
                                    case FinalEP(_, ImmutableType) ⇒
                                    case FinalEP(_, _) ⇒
                                        return Result(definedMethod, LBImpure);
                                    case ep ⇒
                                        dependees += ep
                                }
                            }
                            if (field.isNotFinal) {
                                propertyStore(field, FieldMutability.key) match {
                                    case FinalEP(_, _: FinalField) ⇒
                                    case FinalEP(_, _) ⇒
                                        return Result(definedMethod, LBImpure);
                                    case ep ⇒
                                        dependees += ep
                                }
                            }

                        case _ ⇒
                            // We know nothing about the target field (it is not
                            // found in the scope of the current project).
                            return Result(definedMethod, LBImpure);
                    }

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode ⇒ instruction match {

                    case MethodInvocationInstruction(`declaringClassType`, _, `methodName`, `methodDescriptor`) ⇒
                    // We have a self-recursive call; such calls do not influence
                    // the computation of the method's purity and are ignored.
                    // Let's continue with the evaluation of the next instruction.

                    case mii: NonVirtualMethodInvocationInstruction ⇒

                        nonVirtualCall(mii) match {

                            case Success(callee) ⇒
                                /* Recall that self-recursive calls are handled earlier! */
                                val purity = propertyStore(declaredMethods(callee), Purity.key)

                                purity match {
                                    case FinalEP(_, CompileTimePure | LBPure) ⇒ /* Nothing to do */

                                    // Handling cyclic computations
                                    case ep @ IntermediateEP(_, _, LBPure) ⇒
                                        dependees += ep

                                    case EPS(_, _, _) ⇒
                                        return Result(definedMethod, LBImpure);

                                    case epk ⇒
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ ⇒
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return Result(definedMethod, LBImpure);

                        }
                }

                //TODO Should we still treat new/newarray as impure?
                case NEW.opcode |
                    GETFIELD.opcode |
                    PUTFIELD.opcode | PUTSTATIC.opcode |
                    NEWARRAY.opcode | MULTIANEWARRAY.opcode | ANEWARRAY.opcode |
                    AALOAD.opcode | AASTORE.opcode |
                    BALOAD.opcode | BASTORE.opcode |
                    CALOAD.opcode | CASTORE.opcode |
                    SALOAD.opcode | SASTORE.opcode |
                    IALOAD.opcode | IASTORE.opcode |
                    LALOAD.opcode | LASTORE.opcode |
                    DALOAD.opcode | DASTORE.opcode |
                    FALOAD.opcode | FASTORE.opcode |
                    ARRAYLENGTH.opcode |
                    MONITORENTER.opcode | MONITOREXIT.opcode |
                    INVOKEDYNAMIC.opcode | INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode ⇒
                    return Result(definedMethod, LBImpure);

                case ARETURN.opcode |
                    IRETURN.opcode | FRETURN.opcode | DRETURN.opcode | LRETURN.opcode |
                    RETURN.opcode ⇒
                // if we have a monitor instruction the method is impure anyway..
                // hence, we can ignore the monitor related implicit exception

                case _ ⇒
                    // All other instructions (IFs, Load/Stores, Arith., etc.) are pure
                    // as long as no implicit exceptions are raised.
                    if (instruction.jvmExceptions.nonEmpty) {
                        // JVM Exceptions reify the stack and, hence, make the method impure as
                        // the calling context is now an explicit part of the method's result.
                        return Result(definedMethod, LBImpure);
                    }
                // else ok..

            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        // IN GENERAL
        // Every method that is not identified as being impure is (conditionally)pure.
        if (dependees.isEmpty)
            return Result(definedMethod, LBPure);

        // This function computes the “purity for a method based on the properties of its dependees:
        // other methods (Purity), types (immutability), fields (effectively final)
        def c(eps: SomeEPS): PropertyComputationResult = {
            // Let's filter the entity.
            dependees = dependees.filter(_.e ne eps.e)

            eps match {
                // We can't report any real result as long as we don't know that the fields are all
                // effectively final and the types are immutable.

                case FinalEP(_, _: FinalField | ImmutableType) ⇒
                    if (dependees.isEmpty) {
                        Result(definedMethod, LBPure)
                    } else {
                        // We still have dependencies regarding field mutability/type immutability;
                        // hence, we have nothing to report.
                        IntermediateResult(definedMethod, LBImpure, LBPure, dependees, c)
                    }

                case FinalEP(_, ImmutableContainerType) ⇒
                    Result(definedMethod, LBImpure)

                // The type is at most conditionally immutable.
                case FinalEP(_, _: TypeImmutability) ⇒ Result(definedMethod, LBImpure)
                case FinalEP(_, _: NonFinalField)    ⇒ Result(definedMethod, LBImpure)

                case FinalEP(_, CompileTimePure | LBPure) ⇒
                    if (dependees.isEmpty)
                        Result(definedMethod, LBPure)
                    else {
                        IntermediateResult(definedMethod, LBImpure, LBPure, dependees, c)
                    }

                case IntermediateEP(_, _, _) ⇒
                    dependees += eps
                    IntermediateResult(definedMethod, LBImpure, LBPure, dependees, c)

                case FinalEP(_, _: Purity) ⇒
                    // a called method is impure...
                    Result(definedMethod, LBImpure)
            }
        }

        IntermediateResult(definedMethod, LBImpure, LBPure, dependees, c)
    }

    def determinePurityStep1(definedMethod: DefinedMethod): PropertyComputationResult = {
        val method = definedMethod.definedMethod

        // All parameters either have to be base types or have to be immutable.
        // IMPROVE Use plain object type once we use ObjectType in the store!
        var referenceTypes = method.parameterTypes.iterator.collect {
            case t: ObjectType ⇒ t
            case _: ArrayType  ⇒ return Result(definedMethod, LBImpure);
        }
        val methodReturnType = method.descriptor.returnType
        if (methodReturnType.isArrayType) {
            // we currently have no logic to decide whether the array was created locally
            // and did not escape or was created elsewhere...
            return Result(definedMethod, LBImpure);
        }
        if (methodReturnType.isObjectType) {
            referenceTypes ++= Iterator(methodReturnType.asObjectType)
        }

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty
        referenceTypes foreach { e ⇒
            propertyStore(e, TypeImmutability.key) match {
                case FinalEP(_, ImmutableType) ⇒ /*everything is Ok*/
                case FinalEP(_, _) ⇒
                    return Result(definedMethod, LBImpure);
                case eps @ IntermediateEP(_, _, ub) if ub ne ImmutableType ⇒
                    return Result(definedMethod, LBImpure);
                case epk ⇒ dependees += epk
            }
        }

        doDeterminePurityOfBody(definedMethod, dependees)
    }

    /**
     * Determines the purity of the given method.
     */
    def determinePurity(definedMethod: DeclaredMethod): PropertyComputationResult = {
        val DefinedMethod(_, method) = definedMethod

        if (method.isSynchronized)
            return Result(definedMethod, LBImpure);

        // 1. step (will schedule 2. step if necessary):
        determinePurityStep1(definedMethod.asDefinedMethod)
    }

    /** Called when the analysis is scheduled lazily. */
    def doDeterminePurity(e: Entity): PropertyComputationResult = {
        e match {
            case m: DefinedMethod         ⇒ determinePurity(m)
            case m: VirtualDeclaredMethod ⇒ Result(m, Impure)
            case e                        ⇒ throw new UnknownError("purity is only defined for methods")
        }
    }
}

trait L0PurityAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(Purity)

    override def uses: Set[PropertyKind] = Set(TypeImmutability, FieldMutability)
}

object EagerL0PurityAnalysis extends L0PurityAnalysisScheduler with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L0PurityAnalysis(project)
        propertyStore.scheduleForEntities(project.get(DeclaredMethodsKey).declaredMethods)(analysis.determinePurity)
        analysis
    }
}

object LazyL0PurityAnalysis extends L0PurityAnalysisScheduler with FPCFLazyAnalysisScheduler {
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new L0PurityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(Purity.key, analysis.doDeterminePurity)
        analysis
    }
}
