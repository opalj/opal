/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package fpa

import scala.language.postfixOps
import java.net.URL
import org.opalj.br.PC
import org.opalj.br.Method
import org.opalj.br.analyses.{ Project, SomeProject }
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.MONITORENTER
import org.opalj.br.instructions.MONITOREXIT
import org.opalj.br.instructions.NEW
import org.opalj.br.instructions.NEWARRAY
import org.opalj.br.instructions.MULTIANEWARRAY
import org.opalj.br.instructions.ANEWARRAY
import org.opalj.br.instructions.AALOAD
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ARRAYLENGTH
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
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.fp.{ Entity, Property, PropertyComputationResult, PropertyStore, PropertyKey }
import org.opalj.fp.EOptionP
import org.opalj.fp.EP
import org.opalj.fp.EPK
import org.opalj.fp.Unchanged
import org.opalj.fp.Impossible
import org.opalj.fp.Continuation
import org.opalj.fp.Result
import org.opalj.fp.ImmediateResult
import org.opalj.fp.IntermediateResult

/**
 * This analysis determines whether a method is pure (I.e., Whether the method
 * only operates on the given state.) This simple analysis only tries to compute the
 * purity for methods that
 * only have parameters with a base type.
 *
 * @author Michael Eichberg
 */
object PurityAnalysis {

    final val Purity = org.opalj.fpa.Purity.Key

    final val Mutability = org.opalj.fpa.Mutability.Key

    /*
     * Determines the purity of the method starting with the instruction with the given
     * pc. If the given pc is larger than 0 then all previous instructions must be pure.
     *
     * This function encapsulates the continuation.
     */
    private def determinePurityCont(
        method: Method,
        pc: PC,
        dependees: Set[EOptionP])(
            implicit project: SomeProject,
            projectStore: PropertyStore): PropertyComputationResult = {

        val declaringClassType = project.classFile(method).thisType
        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.size

        var currentPC = pc
        var currentDependees = dependees

        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)

            (instruction.opcode: @scala.annotation.switch) match {
                case GETSTATIC.opcode ⇒
                    val GETSTATIC(declaringClass, fieldName, fieldType) = instruction
                    import project.classHierarchy.resolveFieldReference
                    resolveFieldReference(declaringClass, fieldName, fieldType, project) match {

                        case Some(field) if field.isFinal ⇒
                        /* Nothing to do; constants do not impede purity! */

                        case Some(field) if field.isPrivate /*&& field.isNonFinal*/ ⇒
                            val c: Continuation =
                                (dependeeE: Entity, dependeeP: Property) ⇒
                                    if (dependeeP == EffectivelyFinal) {
                                        val nextPC = body.pcOfNextInstruction(currentPC)
                                        determinePurityCont(method, nextPC, dependees)
                                    } else {
                                        Result(method, Impure)
                                    }
                            // We are suspending this computation and wait for the result.
                            // This, however, does not make this a multi-step computation,
                            // as the analysis is only continued when property becomes
                            // available.
                            import projectStore.require
                            return require(method, Purity, field, Mutability)(c);

                        case _ ⇒
                            return ImmediateResult(method, Impure);
                    }

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode ⇒ instruction match {

                    case MethodInvocationInstruction(`declaringClassType`, `methodName`, `methodDescriptor`) ⇒
                    // We have a self-recursive call; such calls do not influence
                    // the computation of the method's purity and are ignored.
                    // Let's continue with the evaluation of the next instruction.

                    case MethodInvocationInstruction(declaringClassType, methodName, methodDescriptor) ⇒
                        import project.classHierarchy.lookupMethodDefinition
                        val calleeOpt =
                            lookupMethodDefinition(
                                declaringClassType.asObjectType /* this is safe...*/ ,
                                methodName,
                                methodDescriptor,
                                project)
                        calleeOpt match {
                            case None ⇒
                                // We know nothing about the target method (it is not
                                // found in the scope of the currenr project).
                                return ImmediateResult(method, Impure);

                            case Some(callee) ⇒
                                /* Recall that self-recursive calls are handled earlier! */
                                val purity = projectStore(callee, Purity)

                                purity match {
                                    case Some(Pure)   ⇒ /* Nothing to do...*/
                                    case Some(Impure) ⇒ return ImmediateResult(method, Impure);

                                    // Handling cyclic computations
                                    case Some(ConditionallyPure) ⇒
                                        currentDependees += EP(callee, ConditionallyPure)

                                    case None ⇒
                                        currentDependees += EPK(callee, Purity)

                                    case _ ⇒
                                        val message = s"unknown purity $purity"
                                        throw new UnknownError(message)
                                }
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
                /* All other instructions (IFs, Load/Stores, Arith., etc.) are pure. */
            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        // Every method that is not identified as being impure is (conditionally)pure.
        if (currentDependees.isEmpty)
            ImmediateResult(method, Pure)
        else {
            val continuation = new ((Entity, Property) ⇒ PropertyComputationResult) {

                // We use the set of remaining dependencies to test if we have seen
                // all remaining properties.
                var remainingDependendees = currentDependees.map(eOptionP ⇒ eOptionP.e)

                def apply(e: Entity, p: Property): PropertyComputationResult = this.synchronized {
                    if (remainingDependendees.isEmpty)
                        return Unchanged;

                    p match {
                        case Impure ⇒
                            remainingDependendees = Set.empty
                            Result(method, Impure)

                        case Pure ⇒
                            remainingDependendees -= e
                            if (remainingDependendees.isEmpty) {
                                Result(method, Pure)
                            } else
                                Unchanged

                        case MaybePure ⇒
                            // In this case the framework "terminated" this computation
                            // because it is waiting on a result that will never come
                            // because no more tasks are scheduled.
                            remainingDependendees = Set.empty
                            Result(method, Impure)

                        case ConditionallyPure ⇒ Unchanged
                    }
                }
            }
            IntermediateResult(method, ConditionallyPure, currentDependees, continuation)
        }
    }

    /**
     * Determines the purity of the given method.
     */
    def determinePurity(
        method: Method)(
            implicit project: SomeProject, store: PropertyStore): PropertyComputationResult = {

        /* FOR TESTING PURPOSES!!!!! */ if (method.name == "cpure")
            /* FOR TESTING PURPOSES!!!!! */ return Impossible;

        // Due to a lack of knowledge, we classify all native methods or methods loaded
        // using a library class loader as impure...
        if (method.body.isEmpty /*HERE: method.isNative*/ )
            return ImmediateResult(method, Impure);

        // We are currently only able to handle simple methods that just take
        // primitive values.
        if (method.parameterTypes.exists { !_.isBaseType })
            return ImmediateResult(method, Impure);

        val purity = determinePurityCont(method, 0, Set.empty)
        purity
    }

    def analyze(implicit project: Project[URL]): Unit = {
        implicit val projectStore = project.get(SourceElementsPropertyStoreKey)
        val entitySelector: PartialFunction[Entity, Method] = {
            case m: Method if !m.isAbstract ⇒ m
        }
        projectStore <||< (entitySelector, determinePurity)

        //        // Ordering by size (implicit assumption: methods that are short don't call
        //        // too many other methods...); this ordering is extremely efficient and simple.
        //        val methodOrdering = new Ordering[Method] {
        //            def compare(a: Method, b: Method): Int = {
        //                val aBody = a.body
        //                val bBody = b.body
        //                val aSize = if (aBody.isEmpty) -1 else aBody.get.instructions.size
        //                val bSize = if (bBody.isEmpty) -1 else bBody.get.instructions.size
        //                bSize - aSize
        //            }
        //        }
        //        projectStore <||~< (entitySelector, methodOrdering, determinePurity)

    }
}

