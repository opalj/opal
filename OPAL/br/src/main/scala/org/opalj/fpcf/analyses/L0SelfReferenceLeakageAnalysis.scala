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

import org.opalj.log.OPALLogger.{debug ⇒ trace}

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.fpcf.properties.DoesNotLeakSelfReference
import org.opalj.fpcf.properties.LeaksSelfReference
import org.opalj.fpcf.properties.SelfReferenceLeakage

/**
 * A shallow analysis that determines for each new object that is created within a method
 * if it escapes the scope of the method.
 *
 * An object escapes the scope of a method if:
 *  - ... it is assigned to a field,
 *  - ... it is passed to a method,
 *  - ... it is stored in an array,
 *  - ... it is returned,
 *  - ... the object itself leaks it's self reference (`this`) by:
 *      - ... storing `this` in some static field or,
 *      - ... storing it's self reference in a data-structure (another object or array)
 *        passed to it (by assigning to a field or calling a method),
 *      - ... if a superclass leaks the self reference.
 *
 * This analysis can be used as a foundation for an analysis that determines whether
 * all instances created for a specific class never escape the creating method and,
 * hence, respective types cannot occur outside the respective contexts.
 *
 * @author Michael Eichberg
 */
class L0SelfReferenceLeakageAnalysis(
        val project: SomeProject,
        val debug:   Boolean
) extends FPCFAnalysis {

    val SelfReferenceLeakage = org.opalj.fpcf.properties.SelfReferenceLeakage.Key

    /**
     * Analyzes the given method to determine if it leaks the self reference.
     *
     * Currently, this method just returns `true`; it is intended to be overriden by
     * subclasses.
     */
    def leaksSelfReference(method: Method): Boolean = {
        // e.g., use AI()...
        return true;
    }

    /**
     * Determines for the given class file if any method may leak the self reference (`this`).
     *
     * Hence, it only makes sense to call this method if all supertypes do not leak
     * their self reference.
     */
    private[this] def determineSelfReferenceLeakageContinuation(
        classFile:       ClassFile,
        immediateResult: Boolean
    ): PropertyComputationResult = {

        import project.logContext

        val classType = classFile.thisType
        val classHierarchy = project.classHierarchy

        def thisIsSubtypeOf(otherType: ObjectType): Boolean = {
            classHierarchy.isSubtypeOf(classType, otherType.asObjectType).isYesOrUnknown
        }

        // This method just implements a very quick check if there is any potential
        // that the method may leak it's self reference. Hence, if this method returns
        // true, a more thorough analysis is useful/necessary.
        def potentiallyLeaksSelfReference(method: Method): Boolean = {
            val returnType = method.returnType
            if (returnType.isObjectType && thisIsSubtypeOf(returnType.asObjectType))
                return true;
            implicit val code = method.body.get
            val instructions = code.instructions
            val max = instructions.length
            var pc = 0
            while (pc < max) {
                val instruction = instructions(pc)
                instruction.opcode match {
                    case AASTORE.opcode ⇒
                        return true;
                    case ATHROW.opcode if thisIsSubtypeOf(ObjectType.Throwable) ⇒
                        // the exception may throw itself...
                        return true;
                    case INVOKEDYNAMIC.opcode ⇒
                        return true;
                    case INVOKEINTERFACE.opcode |
                        INVOKESPECIAL.opcode |
                        INVOKESTATIC.opcode |
                        INVOKEVIRTUAL.opcode ⇒
                        val invoke = instruction.asInstanceOf[MethodInvocationInstruction]
                        val parameterTypes = invoke.methodDescriptor.parameterTypes
                        if (parameterTypes.exists { pt ⇒ pt.isObjectType && thisIsSubtypeOf(pt.asObjectType) })
                            return true;
                    case PUTSTATIC.opcode | PUTFIELD.opcode ⇒
                        val fieldType = instruction.asInstanceOf[FieldWriteAccess].fieldType
                        if (fieldType.isObjectType && thisIsSubtypeOf(fieldType.asObjectType))
                            return true;
                    case _ ⇒ /*nothing to do*/
                }
                pc = instruction.indexOfNextInstruction(pc)
            }

            return false;
        }

        val doesLeakSelfReference =
            classFile.methods exists { m ⇒
                if (m.isNative ||
                    (
                        m.isNotStatic && m.isNotAbstract &&
                        potentiallyLeaksSelfReference(m) && // <= quick check
                        leaksSelfReference(m)
                    )) {
                    if (debug)
                        trace("analysis result", m.toJava("leaks self reference"))
                    true
                } else {
                    if (debug)
                        trace("analysis result", m.toJava("conceales self reference"))
                    false
                }

            }
        if (doesLeakSelfReference) {
            if (debug)
                trace("analysis result", s"${classFile.thisType.toJava} leaks its self reference")
            if (immediateResult)
                Result(classFile, LeaksSelfReference)
            else
                Result(classFile, LeaksSelfReference)
        } else {
            if (debug)
                trace(
                    "analysis result",
                    s"${classFile.thisType.toJava} does not leak its self reference"
                )
            if (immediateResult)
                Result(classFile, DoesNotLeakSelfReference)
            else
                Result(classFile, DoesNotLeakSelfReference)
        }
    }

    def determineSelfReferenceLeakage(classFile: ClassFile): PropertyComputationResult = {

        if (classFile.thisType eq ObjectType.Object) {
            if (debug)
                trace(
                    "analysis result",
                    "java.lang.Object does not leak its self reference [configured]"
                )
            return Result(ObjectType.Object, DoesNotLeakSelfReference);
        }

        // Let's check the direct supertypes w.r.t. their leakage property.
        val superclassType = classFile.superclassType.get
        val interfaceTypes = classFile.interfaceTypes

        // Given that we may have Java 8+, we may have a default method that leaks
        // the self reference.
        val superClassFiles: Seq[ClassFile] =
            if (superclassType ne ObjectType.Object)
                superclassFileOption.get +: interfaceTypesOption.map(_.get)
            else
                Seq.empty

        if (superClassFiles.nonEmpty) {
            if (debug)
                trace(
                    "analysis progress",
                    s"${classFile.thisType.toJava} waiting on leakage information about: ${superClassFiles.map(_.thisType.toJava).mkString(", ")}"
                )
            var dependees = Map.empty[Entity, EOptionP[Entity, SelfReferenceLeakage]]

            propertyStore(superClassFiles, SelfReferenceLeakage) foreach {
                case epk @ EPK(e, _) ⇒ dependees += ((e, epk))

                case eps @ EPS(_, DoesNotLeakSelfReference, isFinal) ⇒
                    if (isFinal)
                        return Result(classFile, LeaksSelfReference);
                    else
                        dependees += ((e, eps))

                case EPS(_, LeaksSelfReference, isFinal) ⇒
                    assert(isFinal)
                    return Result(classFile, LeaksSelfReference);

            }

            if (dependees.isEmpty) {
                determineSelfReferenceLeakageContinuation(classFile, immediateResult = true)
            } else {
                def c(eps: SomeEPS): PropertyComputationResult = {
                    val EPS(e, p, isFinal) = eps
                    p match {
                        case LeaksSelfReference ⇒
                            if (isFinal) {
                                return Result(e, LeaksSelfReference);
                            }
                            dependees = dependees - e + ((e, EPS(e, LeaksSelfReference, isFinal)))

                        case DoesNotLeakSelfReference ⇒
                            dependees = dependees - e
                    }
                    if (dependees.isEmpty)
                        determineSelfReferenceLeakageContinuation(classFile, immediateResult = false)
                    else
                        IntermediateResult(classFile, LeaksSelfReference, dependees.values, c)
                }
                IntermediateResult(classFile, LeaksSelfReference, dependees.values, c)
            }

        } else {
            determineSelfReferenceLeakageContinuation(classFile, immediateResult = true)
        }
    }
}

object L0SelfReferenceLeakageAnalysis extends FPCFEagerAnalysisScheduler {

    def uses: Set[PropertyKind] = Set.empty

    def derives: Set[PropertyKind] = Set(SelfReferenceLeakage.Key)

    /**
     * Starts the analysis for the given `project`. This method is typically implicitly
     * called by the [[FPCFAnalysesManager]].
     */
    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val config = project.config
        val debug = config.getBoolean("org.opalj.fcpf.analysis.L0SelfReferenceLeakage.debug")
        val analysis = new L0SelfReferenceLeakageAnalysis(project, debug)
        import analysis.determineSelfReferenceLeakage
        import project.allProjectClassFiles
        propertyStore.scheduleForEntities(allProjectClassFiles)(determineSelfReferenceLeakage)
        analysis
    }

}
