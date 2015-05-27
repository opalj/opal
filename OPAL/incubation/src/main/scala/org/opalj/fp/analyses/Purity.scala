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

case object Pure extends Purity

case object Impure extends Purity

sealed trait Mutability extends Property {
    final val key = Mutability.Key // All instances have to share the SAME key!
}

private object Mutability {
    final val Key = PropertyKey.create("Mutability")
}

case object EffectivelyFinal extends Mutability

case object NonFinal extends Mutability

/**
 * This analysis determines whether a method is pure (I.e., Whether the method
 * only operates on the given state.) This simple analysis only considers methods that
 * only have parameters with a base type.
 * This needs a fixpoint computation as a method that only calls other pure methods
 * is also pure.
 */
object PurityAnalysis extends DefaultOneStepAnalysis {

    override def title: String =
        "determines those methods that are pure"

    override def description: String =
        "identifies method which are pure; i.e. which just operate on the passed parameters"

    /* The implementation is inspired by continuation-passing style.
     * Here, the rest of the computation is encapsulated by a PropertyComputation object.
     */

    /*
     * Determines the purity of the method starting with the instruction with the given
     * pc.
     */
    def determinePurityCont(
        pc: PC)(
            method: Method)(
                implicit project: SomeProject,
                projectStore: PropertyStore): PropertyComputationResult = {

        val declaringClassType = project.classFile(method).thisType
        val methodDescriptor = method.descriptor
        val methodName = method.name

        val debug = declaringClassType == org.opalj.br.ObjectType("java/util/Optional") && methodName == "hashCode"

        val body = method.body.get
        val instructions = body.instructions
        val maxPC = body.instructions.size

        var currentPC = pc

        /*
         * Create a representation of the rest of the computation that needs to be
         * carried out once the property becomes known.
         */
        def waitOnPurityInformation(callee: Method): Suspended = {
            new Suspended(method, Purity.Key, callee, Purity.Key) {
                Console.err.println(":::::::::::::::::: created "+this.toString())
                final val nextPC = body.pcOfNextInstruction(currentPC)

                def continue(
                    dependingEntity: AnyRef,
                    dependingProperty: Property): PropertyComputationResult = {

                    if (dependingProperty == Pure) {
                        determinePurityCont(nextPC)(method)
                    } else {
                        Result(method, Impure)
                    }
                }

                def terminate(): Unit = { /* Nothing to do. */ }

                def fallback: Purity = Impure

                override def toString: String =
                    "suspended purity computation of "+
                        method.toJava(declaringClassType)+
                        "; requiring purity information about "+
                        callee.toJava(project.classFile(callee))
            }
        }

        /*
         * Create a representation of the rest of the computation that needs to be
         * carried out once the property becomes known.
         */
        def waitOnMutabilityInformation(field: Field): Suspended = {
            new Suspended(method, Purity.Key, field, Mutability.Key) {
                Console.err.println(":::::::::::::::::: created "+this.toString())
                final val nextPC = body.pcOfNextInstruction(currentPC)

                def continue(
                    dependingEntity: AnyRef,
                    dependingProperty: Property): PropertyComputationResult = {

                    if (dependingProperty == EffectivelyFinal) {
                        determinePurityCont(nextPC)(method)
                    } else {
                        Result(method, Impure)
                    }
                }

                def terminate(): Unit = { /* Nothing to do. */ }

                def fallback: Mutability = NonFinal

                override def toString: String =
                    "suspended purity computation of "+
                        method.toJava(declaringClassType)+
                        "; requiring mutability information about "+
                        field.toJava(project.classFile(field))
            }
        }

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
                        // nothing to do... (we don't care about constants)

                        case Some(field) if field.isPrivate /*&& field.isNonFinal*/ ⇒
                            val mutability = projectStore(field, Mutability.Key)
                            Console.err.println(s"mutability of callee $field is $mutability")
                            mutability match {
                                case Some(EffectivelyFinal) ⇒ /* Nothing to do...*/
                                case Some(NonFinal)         ⇒ return Result(method, Impure);
                                case None                   ⇒ return waitOnMutabilityInformation(field);
                                case _                      ⇒ new UnknownError
                            }

                        case _ ⇒ return Result(method, Impure);
                    }

                case NEW.opcode |
                    GETFIELD.opcode |
                    PUTFIELD.opcode | PUTSTATIC.opcode |
                    MONITORENTER.opcode | MONITOREXIT.opcode ⇒
                    return Result(method, Impure);

                case NEWARRAY.opcode | MULTIANEWARRAY.opcode | ANEWARRAY.opcode |
                    AALOAD.opcode | AASTORE.opcode |
                    BALOAD.opcode | BASTORE.opcode |
                    CALOAD.opcode | CASTORE.opcode |
                    SALOAD.opcode | SASTORE.opcode |
                    IALOAD.opcode | IASTORE.opcode |
                    LALOAD.opcode | LASTORE.opcode |
                    DALOAD.opcode | DASTORE.opcode |
                    FALOAD.opcode | FASTORE.opcode |
                    ARRAYLENGTH.opcode ⇒
                    return Result(method, Impure);

                case INVOKEDYNAMIC.opcode ⇒
                    return Result(method, Impure);

                case INVOKEVIRTUAL.opcode | INVOKEINTERFACE.opcode ⇒
                    return Result(method, Impure);

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
                                    val purity = projectStore(callee, Purity.Key)
                                    Console.err.println(s"purity of callee $callee is $purity")
                                    purity match {
                                        case Some(Pure)   ⇒ /* Nothing to do...*/
                                        case Some(Impure) ⇒ return Result(method, Impure);
                                        case None         ⇒ return waitOnPurityInformation(callee);

                                        case _            ⇒ throw new UnknownError
                                    }
                            }
                    }

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
            implicit project: SomeProject,
            projectStore: PropertyStore): PropertyComputationResult = {
        if (!entity.isInstanceOf[Method])
            return Impossible;

        val method = entity.asInstanceOf[Method]
        if (method.isAbstract)
            return Impossible;

        /* FOR TESTING PURPOSES!!!!! */ if (method.name == "cpure")
            /* FOR TESTING PURPOSES!!!!! */ return Impossible;

        // Due to a lack of knowledge, we classify all native methods as impure...
        if (method.body.isEmpty)
            return Result(method, Impure);

        // We are currently only able to handle simple methods that just take
        // primitive values.
        if (method.parameterTypes.exists { !_.isBaseType })
            return Result(method, Impure);

        determinePurityCont(0)(method)
    }

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineMutabilityOfNonFinalPrivateStaticFields(
        entity: AnyRef)(
            implicit project: SomeProject,
            projectStore: PropertyStore): PropertyComputationResult = {
        if (!entity.isInstanceOf[ClassFile])
            return Impossible;

        val classFile = entity.asInstanceOf[ClassFile]
        val thisType = classFile.thisType

        val psnfFields = classFile.fields.filter(f ⇒ f.isPrivate && f.isStatic && !f.isFinal).toSet
        var effectivelyFinalFields = psnfFields
        if (psnfFields.isEmpty)
            return Empty;

        val concreteStaticMethods = classFile.methods filter { m ⇒
            m.isStatic && !m.isStaticInitializer && !m.isNative
        }
        concreteStaticMethods foreach { m ⇒
            m.body.get foreach { (pc, instruction) ⇒
                instruction match {
                    case PUTSTATIC(`thisType`, fieldName, fieldType) ⇒
                        // we don't need to lookup the field in the
                        // the class hierarchy since we are only concerned about private
                        // fiels so far... so we don't have to do a
                        // classHierarchy.resolveFieldReference(thisType, fieldName, fieldType, project).get
                        classFile.findField(fieldName) foreach { f ⇒ effectivelyFinalFields -= f }
                    case _ ⇒ /*Nothing to do*/
                }
            }
        }

        /*DEBUGGING*/ if (psnfFields.nonEmpty)
            /*DEBUGGING*/ println(psnfFields.map(f ⇒ f.toJava(thisType) + effectivelyFinalFields.contains(f)).toSeq.sorted.mkString("\n"))

        Result(
            psnfFields map { f ⇒
                if (effectivelyFinalFields.contains(f))
                    (f, EffectivelyFinal)
                else
                    (f, NonFinal)
            }
        )
    }

    override def doAnalyze(
        project: Project[URL],
        parameters: Seq[String] = List.empty,
        isInterrupted: () ⇒ Boolean): BasicReport = {

        implicit val theProjectStore = PropertyStore(project.allSourceElements)
        implicit val theProject = project

        theProjectStore <<= determineMutabilityOfNonFinalPrivateStaticFields
        theProjectStore <<= determinePurity

        theProjectStore waitOnPropertyComputationCompletion

        val pureMethods: Traversable[(Method, Property)] =
            theProjectStore(Purity.Key).filter(_._2 == Pure).map(e ⇒ (e._1.asInstanceOf[Method], e._2))
        val pureMethodsCount = pureMethods.size
        val pureMethodsAsStrings = pureMethods.map(m ⇒ m._1.toJava(project.classFile(m._1)))
        BasicReport(
            pureMethodsAsStrings.toList.sorted.mkString("\nPure methods:\n", "\n", s"\nTotal: $pureMethodsCount\n") +
                theProjectStore.toString
        )
    }
}

