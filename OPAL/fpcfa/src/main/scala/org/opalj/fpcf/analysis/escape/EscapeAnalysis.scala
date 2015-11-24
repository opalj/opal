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
package fpcf
package analysis
package escape

import java.net.URL
import net.ceedubs.ficus.Ficus._
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.INVOKEDYNAMIC
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.log.OPALLogger

/**
 * A shallow analysis that determines for each object that is created within a method (new)
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
 * hence, respective types cannot occur.
 *
 * @author Michael Eichberg
 */
class EscapeAnalysis(val debug: Boolean) {

    val SelfReferenceLeakage = org.opalj.fpcf.analysis.escape.SelfReferenceLeakage.Key

    /**
     * Determines for the given class file if any method may leak the self reference (`this`).
     *
     * Hence, it only makes sense to call this method if all supertypes do not leak
     * their self reference.
     */
    private[this] def determineSelfReferenceLeakageContinuation(
        classFile:       ClassFile,
        immediateResult: Boolean
    )(
        implicit
        project: SomeProject, store: PropertyStore
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
            val body = method.body.get
            val instructions = body.instructions
            val max = instructions.length
            var pc = 0
            while (pc < max) {
                val instruction = instructions(pc)
                instruction.opcode match {
                    case AASTORE.opcode ⇒
                        return true;
                    case ATHROW.opcode if thisIsSubtypeOf(ObjectType.Throwable) ⇒
                        // the exception throws itself...
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
                pc = instruction.indexOfNextInstruction(pc, body)
            }

            return false;
        }

        // This method performs a thorough data-flow analysis to determine if the self reference
        // (`this`) is eventually leaked.
        def leaksSelfReference(method: Method): Boolean = {
            //    AI()
            return true;
        }

        val doesLeakSelfReference =
            classFile.methods exists { m ⇒
                if (m.isNative ||
                    (m.isNotStatic && m.isNotAbstract &&
                        (potentiallyLeaksSelfReference(m) && leaksSelfReference(m)))) {
                    if (debug)
                        OPALLogger.debug(
                            "analysis result",
                            s"${m.toJava(classFile)} leaks its self reference"
                        )
                    true
                } else {
                    if (debug)
                        OPALLogger.debug(
                            "analysis result",
                            s"${m.toJava(classFile)} does not leak its self reference"
                        )
                    false
                }

            }
        if (doesLeakSelfReference) {
            if (debug)
                OPALLogger.debug(
                    "analysis result",
                    s"${classFile.thisType.toJava} leaks its self reference"
                )
            if (immediateResult)
                ImmediateResult(classFile, LeaksSelfReference)
            else
                Result(classFile, LeaksSelfReference)
        } else {
            if (debug)
                OPALLogger.debug(
                    "analysis result",
                    s"${classFile.thisType.toJava} does not leak its self reference"
                )
            if (immediateResult)
                ImmediateResult(classFile, DoesNotLeakSelfReference)
            else
                Result(classFile, DoesNotLeakSelfReference)
        }
    }

    def determineSelfReferenceLeakage(
        classFile: ClassFile
    )(
        implicit
        project: SomeProject, store: PropertyStore
    ): PropertyComputationResult = {
        import project.logContext

        if (classFile.thisType eq ObjectType.Object) {
            if (debug)
                OPALLogger.debug(
                    "analysis result",
                    "java.lang.Object does not leak its self reference [configured]"
                )
            return ImmediateResult(classFile, DoesNotLeakSelfReference);
        }

        // Let's check the supertypes w.r.t. their leakage property.
        val superclassType = classFile.superclassType.get
        val superclassFileOption = project.classFile(superclassType)
        val interfaceTypesOption = classFile.interfaceTypes.map(project.classFile(_))
        val hasUnknownSuperClass = (superclassFileOption.isEmpty && (superclassType ne ObjectType.Object))
        if (hasUnknownSuperClass || interfaceTypesOption.exists(_ == None)) {
            // The project is not complete, hence, we have to use the fallback.
            if (debug)
                OPALLogger.debug(
                    "analysis result",
                    s"${classFile.thisType.toJava} leaks self reference [super type information is incomplete]"
                )
            return ImmediateResult(classFile, LeaksSelfReference);
        }

        // Given that we have Java 8, we may have a default method that leaks
        // the self reference.
        val superClassFiles =
            if (superclassType ne ObjectType.Object)
                superclassFileOption.get +: interfaceTypesOption.map(_.get)
            else
                interfaceTypesOption.map(_.get)

        if (superClassFiles.nonEmpty) {
            if (debug)
                OPALLogger.debug(
                    "analysis progress",
                    s"${classFile.thisType.toJava} waiting on leakage information about: ${superClassFiles.map(_.thisType.toJava).mkString(", ")}"
                )
            store.allHaveProperty(
                /*depender*/ classFile, SelfReferenceLeakage,
                /*dependees*/ superClassFiles, DoesNotLeakSelfReference
            ) { haveProperty ⇒
                if (haveProperty) {
                    determineSelfReferenceLeakageContinuation(classFile, immediateResult = false)
                } else {
                    if (debug)
                        OPALLogger.debug(
                            "analysis result",
                            s"${classFile.thisType.toJava} leaks its self reference [a supertype already leaks the self reference]"
                        )
                    Result(classFile, LeaksSelfReference);
                }
            }
        } else {
            determineSelfReferenceLeakageContinuation(classFile, immediateResult = true)
        }
    }
}

object EscapeAnalysis {

    def analyze(implicit project: SomeProject): Unit = {
        implicit val store = project.get(SourceElementsPropertyStoreKey)
        val filter: PartialFunction[Entity, ClassFile] = { case cf: ClassFile ⇒ cf }
        val debug = project.config.as[Option[Boolean]]("org.opalj.fcpf.analysis.escape.debug")
        val analysis = new EscapeAnalysis(debug.getOrElse(false))
        store <||< (filter, analysis.determineSelfReferenceLeakage)
    }
}

