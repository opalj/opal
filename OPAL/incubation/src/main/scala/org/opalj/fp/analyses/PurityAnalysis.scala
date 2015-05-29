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
package org.opalj.fp
package analyses

import scala.language.postfixOps
import java.net.URL
import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.br.ClassFile
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
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.PC
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.ClassFile

sealed trait Purity extends Property {
    final val key = Purity.Key // All instances have to share the SAME key!
}
private object Purity {
    final val Key = PropertyKey.create("Purity")

}

/*
 * Used if we have a computation
 *
 * Effectively the same as Impure unless it is refined later on.
 */
case object ConditionallyPure extends Purity

case object Pure extends Purity

case object Impure extends Purity

/**
 * This analysis determines whether a method is pure (I.e., Whether the method
 * only operates on the given state.) This simple analysis only considers methods that
 * only have parameters with a base type.
 * This needs a fixpoint computation as a method that only calls other pure methods
 * is also pure.
 */
object PurityAnalysis {

    /* The implementation is inspired by continuation-passing style.
     * Here, the rest of the computation is encapsulated by a PropertyComputation object.
     */

    final val Purity = org.opalj.fp.analyses.Purity.Key

    final val Mutability = org.opalj.fp.analyses.Mutability.Key

    /*
     * Determines the purity of the method starting with the instruction with the given
     * pc. If the given pc is larger than 0 then all previous instructions must be pure.
     *
     * This function encapsulates the continuation.
     */
    def determinePurityCont(
        method: Method,
        pc: PC)(
            implicit project: SomeProject, projectStore: PropertyStore): PropertyComputationResult = {

        val declaringClassType = project.classFile(method).thisType
        val methodDescriptor = method.descriptor
        val methodName = method.name
        val body = method.body.get
        val instructions = body.instructions
        val maxPC = instructions.size

        val debug = declaringClassType == org.opalj.br.ObjectType("java/util/Optional") && methodName == "hashCode"

        var currentPC = pc

        /*
         * Create a representation of the rest of the computation that needs to be
         * carried out once the property becomes known.
         */
        def waitOnPurityInformation(callee: Method): Suspended = {
            new Suspended(method, Purity, callee, Purity) {

                final val nextPC = body.pcOfNextInstruction(currentPC)

                def continue(
                    dependeeE: AnyRef,
                    dependeeP: Property): PropertyComputationResult = {

                    if (dependeeP == Pure) {
                        determinePurityCont(method, nextPC)
                    } else {
                        Result(method, Impure)
                    }
                }

                override def toString: String =
                    "suspended purity computation of "+
                        method.toJava(declaringClassType)+
                        "; requiring purity information about "+
                        callee.toJava(project.classFile(callee))
            }
        }

        //        /*
        //         * Create a representation of the rest of the computation that needs to be
        //         * carried out once the property becomes known.
        //         */
        //        def waitOnMutabilityInformation(field: Field): Suspended = {
        //            new Suspended(method, Purity.Key, field, Mutability.Key) {
        //                Console.err.println(":::::::::::::::::: created "+this.toString())
        //                final val nextPC = body.pcOfNextInstruction(currentPC)
        //
        //                def continue(
        //                    dependingEntity: AnyRef,
        //                    dependingProperty: Property): PropertyComputationResult = {
        //
        //                    if (dependingProperty == EffectivelyFinal) {
        //                        determinePurityCont(nextPC)(method)
        //                    } else {
        //                        Result(method, Impure)
        //                    }
        //                }
        //
        //                override def toString: String =
        //                    "suspended purity computation of "+
        //                        method.toJava(declaringClassType)+
        //                        "; requiring mutability information about "+
        //                        field.toJava(project.classFile(field))
        //            }
        //        }

        while (currentPC < maxPC) {
            val instruction = instructions(currentPC)
            if (debug)
                println(s"\n\n[Debug]${declaringClassType.toJava} $methodName [$currentPC:] $instruction\n")
            (instruction.opcode: @scala.annotation.switch) match {
                case GETSTATIC.opcode ⇒
                    val GETSTATIC(declaringClass, fieldName, fieldType) = instruction
                    import project.classHierarchy.resolveFieldReference
                    resolveFieldReference(declaringClass, fieldName, fieldType, project) match {

                        case Some(field) if field.isFinal ⇒
                        // Nothing to do; constants do not impede purity!

                        case Some(field) if field.isPrivate /*&& field.isNonFinal*/ ⇒

                            //                            val mutability = projectStore(field, Mutability.Key)
                            //                            Console.err.println(s"mutability of callee $field is $mutability")
                            //                            mutability match {
                            //                                case Some(EffectivelyFinal) ⇒ /* Nothing to do...*/
                            //                                case Some(NonFinal)         ⇒ return Result(method, Impure);
                            //                                case None                   ⇒ return waitOnMutabilityInformation(field);
                            //                                case _                      ⇒ new UnknownError
                            //                            }
                            val c: Continuation =
                                (dependeeE: AnyRef, dependeeP: Property) ⇒
                                    if (dependeeP == EffectivelyFinal) {
                                        val nextPC = body.pcOfNextInstruction(currentPC)
                                        determinePurityCont(method, nextPC)
                                    } else {
                                        Result(method, Impure)
                                    }
                            return projectStore.require(method, Purity, field, Mutability)(c);

                        case _ ⇒
                            return Result(method, Impure);
                    }

                case INVOKESPECIAL.opcode | INVOKESTATIC.opcode ⇒
                    instruction match {

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
                                case None ⇒ /*i.e., the target method is not available*/
                                    return Result(method, Impure);
                                case Some(callee) ⇒
                                    /* Recall that self-recursive calls are handled earlier! */
                                    val purity = projectStore(callee, Purity)
                                    Console.err.println(s"purity of callee $callee is $purity")
                                    purity match {
                                        case Some(Pure)   ⇒ /* Nothing to do...*/
                                        case Some(Impure) ⇒ return Result(method, Impure);
                                        case None         ⇒ return waitOnPurityInformation(callee);

                                        case _            ⇒ throw new UnknownError
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
                    return Result(method, Impure);

                case _ ⇒
                /* All other instructions (IFs, Load/Stores, Arith., etc.) are pure. */
            }
            currentPC = body.pcOfNextInstruction(currentPC)
        }

        // Every method that is not identified as being impure is pure.
        Result(method, Pure)
    }

    def determinePurity(
        entity: AnyRef)(
            implicit project: SomeProject, store: PropertyStore): PropertyComputationResult = {
        if (!entity.isInstanceOf[Method])
            return Impossible;

        val method = entity.asInstanceOf[Method]
        if (method.isAbstract)
            return Impossible;

        /* FOR TESTING PURPOSES!!!!! */ if (method.name == "cpure")
            /* FOR TESTING PURPOSES!!!!! */ return Impossible;

        // Due to a lack of knowledge, we classify all native methods of methods loaded
        // using a library class loader as impure...
        if (method.body.isEmpty)
            return Result(method, Impure);

        // We are currently only able to handle simple methods that just take
        // primitive values.
        if (method.parameterTypes.exists { !_.isBaseType })
            return Result(method, Impure);

        determinePurityCont(method, 0)
    }

    def analyze(implicit project: Project[URL]): Unit = {
        implicit val projectStore = project.get(SourceElementsPropertyStoreKey)
        projectStore <<= determinePurity
    }
}

