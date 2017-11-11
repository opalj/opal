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

import scala.annotation.switch
import org.opalj.{Result => CallResult}
import org.opalj.{Failure => CallResolutionFailed}
import org.opalj.{Empty => NoCallTarget}
import org.opalj.{Success => CallTarget}
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger.error
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.FinalField
import org.opalj.fpcf.properties.NonFinalField
import org.opalj.fpcf.properties.Impure
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.MaybePure
import org.opalj.fpcf.properties.ConditionallyPure
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.ImmutableType
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.AtLeastConditionallyImmutableType
import org.opalj.br.PC
import org.opalj.br.Method
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.SourceElement
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NEWARRAY
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.ARRAYLENGTH
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.LALOAD
import org.opalj.br.instructions.IALOAD
import org.opalj.br.instructions.CALOAD
import org.opalj.br.instructions.BALOAD
import org.opalj.br.instructions.BASTORE
import org.opalj.br.instructions.CASTORE
import org.opalj.br.instructions.IASTORE
import org.opalj.br.instructions.LASTORE
import org.opalj.br.instructions.SASTORE
import org.opalj.br.instructions.SALOAD
import org.opalj.br.instructions.DALOAD
import org.opalj.br.instructions.FALOAD
import org.opalj.br.instructions.FASTORE
import org.opalj.br.instructions.DASTORE
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.NonVirtualMethodInvocationInstruction
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE


/**
 * Very simple and fast analysis of the purity of methods as defined by the
 * [[org.opalj.fpcf.properties.Purity]] property.
 *
 * This analysis is a very, very shallow implementation that immediately gives
 * up, when something "complicated" (e.g., method calls which take objects)
 * is encountered.
 *
 * @author Michael Eichberg
 */
class PurityAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {

    import project.resolveFieldReference
    import project.nonVirtualCall

    /**
     * Determines the purity of the method starting with the instruction with the given
     * pc. If the given pc is larger than 0 then all previous instructions (in particular
     * method calls) must not violate this method's purity.
     *
     * This function encapsulates the continuation.
     */
    private[this] def doDeterminePurity(
        method:           Method,
        initialDependees: Set[EOptionP[SourceElement, Property]]
    ): PropertyComputationResult = {

        val declaringClassType = method.classFile.thisType
        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.length

        var dependees = initialDependees
        // The purity level of this method,
        // if all dependees (Fields, Types) have no effect on the purity.
        val lastResult : EOptionP[Method,Purity] = EPK(method,Purity.key)
        var potentialPurityLevel : Purity = ConditionallyPure

        var currentPC = 0
        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            (instruction.opcode: @switch) match {
                case GETSTATIC.opcode ⇒
                    val GETSTATIC(declaringClass, fieldName, fieldType) = instruction

                    resolveFieldReference(declaringClass, fieldName, fieldType) match {

                        // ... we have no support for arrays at the moment
                        case Some(field) if !field.fieldType.isArrayType =>
                            // the field has to be effectively final and
                            // if it is an object – immutable!
                            val fieldType = field.fieldType
                            if(fieldType.isObjectType) {
                                val fieldClassType = project.classFile(fieldType.asObjectType)
                                if(fieldClassType.isDefined)
                                    dependees += EPK(fieldClassType.get,TypeImmutability.key)
                                else
                                    return Result(method, Impure);
                            }
                            if(field.isNotFinal) {
                                dependees += EPK(field, FieldMutability.key)
                            }

                        case _ ⇒
                            // We know nothing about the target field (it is not
                            // found in the scope of the current project).
                            return ImmediateResult(method, Impure);
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
                                val purity = propertyStore(callee, Purity.key)

                                purity match {
                                    case EP(_, Pure) ⇒ /* Nothing to do...*/

                                    case EP(_, Impure | MaybePure) ⇒
                                        return ImmediateResult(method, Impure);

                                    // Handling cyclic computations
                                    case ep @ EP(_, ConditionallyPure) ⇒
                                        dependees += ep

                                    case epk ⇒
                                        dependees += epk
                                }

                            case _ /* Empty or Failure */ ⇒
                                // We know nothing about the target method (it is not
                                // found in the scope of the current project).
                                return ImmediateResult(method, Impure);

                        }
                }

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

                    return ImmediateResult(method, Impure);

                case _ ⇒
                    // All other instructions (IFs, Load/Stores, Arith., etc.) are pure
                    // as long as no implicit exceptions are raised.
                    if(instruction.jvmExceptions.nonEmpty) {
                        // JVM Excpections reify the stack and, hence, make the method impure as
                        // the calling context is now an explicit part of the method's result.
                        return ImmediateResult(method, Impure);
                    }
                    // else ok..

            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        // IN GENERAL
        // Every method that is not identified as being impure is (conditionally)pure.
        if (dependees.isEmpty)
            return ImmediateResult(method, Pure);

        def c(e: Entity, p: Property, ut: UserUpdateType): PropertyComputationResult = {
            // We can't report any real result as long as we don't know that the fields are all
            // effectively final and the types are immutable.
            p match {
                case _ : FinalField | ImmutableType =>
                    // let's filter the entity
                    dependees = dependees.filter(_.e ne e)
                    if(dependees.isEmpty) {
                        Result(method,potentialPurityLevel)
                    } else if(dependees.forall(d => d.pk == Purity.key)) {
                        IntermediateResult(EP(method,potentialPurityLevel),dependees,c)
                    } else {
                        // we still have dependencies regarding field mutability/type immutability
                        IntermediateResult(lastResult,dependees,c)
                    }

                case AtLeastConditionallyImmutableType =>
                    // The result remains as is...
                    IntermediateResult(lastResult,dependees,c)

                case _ : TypeImmutability | _ : NonFinalField =>
                    // the type is either mutable or at most conditionally immutable
                    Result(method, Impure)

                case Impure | MaybePure ⇒
                    Result(method, Impure)

                case ConditionallyPure ⇒
                    val newEP = EP(e.asInstanceOf[Method], p.asInstanceOf[Purity])
                    dependees = dependees.filter(_.e ne e) + newEP
                    IntermediateResult(method, ConditionallyPure, dependees, c)

                case Pure ⇒
                    dependees = dependees.filter { _.e ne e }
                    if (dependees.isEmpty)
                        Result(method, Pure)
                    else
                        IntermediateResult(method, ConditionallyPure, dependees, c)
            }
        }

        IntermediateResult(method, ConditionallyPure, dependees, c)
    }

    protected def doCheckParameterImmutability(
                                                  method : Method
                                              ) :PropertyComputationResult = {

        // All parameters either have to be base types or have to be immutable.
        // IMPROVE Use plain object type once we use ObjectType in the store!
        val referenceTypeParameters = method.parameterTypes.collect{
            case t : ObjectType =>
                project.classFile(t) match {
                    case Some(cf) => cf
                    case None => return ImmediateResult(method, Impure);
                }
        }

        var dependees : Set[EOptionP[SourceElement,Property]] = Set.empty
        referenceTypeParameters foreach { e =>
            propertyStore(e,TypeImmutability.key) match {
                case EP(_,ImmutableType) => /*everything is Ok*/
                case epk : EPK[_,_] =>                    dependees += epk

                case ep @ EP(_,immutability) =>
                    if (immutability.isRefineable)
                        dependees += ep
                    else
                        return ImmediateResult(method, Impure);
            }
        }
        doDeterminePurity(method, dependees)
    }

        /**
     * Determines the purity of the given method.
     */
    def determinePurity(method: Method): PropertyComputationResult = {
        if (method.isSynchronized)
            return ImmediateResult(method, Impure);

        // 1. step ( will schedule 2. step if necessary):
        doCheckParameterImmutability(method)
    }
}

/**
 * @author Michael Eichberg
 */
object PurityAnalysis extends FPCFAnalysisRunner {

    override def derivedProperties: Set[PropertyKind] = Set(Purity)

    override def usedProperties: Set[PropertyKind] = Set(TypeImmutability, FieldMutability)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new PurityAnalysis(project)
        propertyStore.scheduleForEntities(project.allMethodsWithBody)(analysis.determinePurity)
        analysis
    }
}
