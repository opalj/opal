/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package frb
package analyses

import AnalysesHelpers._

import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.instructions._

import org.opalj.ai._
import org.opalj.ai.project._
import org.opalj.ai.domain._
import org.opalj.ai.domain.l1._

/**
 * A domain for FieldIsntImmutableInImmutableClass. This is a DefaultConfigurableDomain
 * with DefaultTypeLevelLongValues instead of DefaultPreciseLongValues.
 *
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
private class ImmutabilityAnalysisDomain[I](val id: I)
        extends Domain
        with DefaultDomainValueBinding
        with ThrowAllPotentialExceptionsConfiguration
        with DefaultPerInstructionPostProcessing
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelIntegerValues
        with DefaultReferenceValuesBinding
        with PredefinedClassHierarchy // FIXME This should not give sufficient information.
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization {

    type Id = I

}

/**
 * Classes annotated with `@Immutable` should be unchanging once constructed in order to
 * guarantee thread-safety. Above all else it means that all fields must be immutable.
 *
 * @author Roberts Kolosovs
 * @author Peter Spieler
 */
class FieldIsntImmutableInImmutableClass[Source]
        extends MultipleResultsAnalysis[Source, SourceLocationBasedReport[Source]] {

    /**
     * Returns a description text for this analysis.
     *
     * @return analysis description.
     */
    def description: String =
        "Reports classes annotated with an annotation with the simple name Immutable"+
            " that contain mutable fields."

    /**
     * Analyzes the given project and returns a list of FieldBasesReports containing all
     * instances of the error.
     *
     * @param project The project to analyze.
     * @param parameters Options for the analysis. Currently unused.
     * @return A list of reports, or an empty list.
     */
    def analyze(
        project: Project[Source],
        parameters: Seq[String] = Seq.empty): Iterable[SourceLocationBasedReport[Source]] = {

        val immutableAnnotationTypes = collectAnnotationTypes(project, "Immutable")

        /**
         * All class files previously encountered and classified and immutable.
         */
        val collectedImmutables = scala.collection.mutable.HashSet.empty[ClassFile]

        /**
         * All class files previously encountered and classified as mutable. Also contains
         * class files where a definitive decision could not be made.
         */
        val collectedMutables = scala.collection.mutable.HashSet.empty[ClassFile]

        /**
         * Objects whose class file could not be fetched for some reason.
         */
        val unknownClassFiles = scala.collection.mutable.Set.empty[ObjectType]

        /**
         * ClassFiles of fields that were already seen in the current cycle of
         * classIsImmutable. Needed to prevent crashes while checking cyclic composition.
         */
        var alreadySeenThisCycle = scala.collection.mutable.Set.empty[ClassFile]

        /**
         * ClassFiles of immutable classes that have fields with cyclic composition.
         */
        val immutableClassesInACycle = scala.collection.mutable.Set.empty[ClassFile]

        /*
         * Evaluates whether a class is immutable.
         *
         * @param classFile The `ClassFile` of the class to be evaluated as mutable or
         * immutable.
         * @return `true` if classFile is immutable, else `false`.
         */
        def classIsImmutable(classFile: ClassFile): Boolean = {
            if (collectedImmutables.contains(classFile)) {
                true
            } else if (collectedMutables.contains(classFile)) {
                false
            } else {
                if (!alreadySeenThisCycle.contains(classFile)) {
                    uncachedClassIsImmutable(classFile)
                } else {
                    immutableClassesInACycle.add(classFile)
                    false
                }
            }
        }

        /*
         * Checks whether the class given in form of an `ObjectType` is immutable.
         *
         * @param objectType The `ObjectType` to check.
         * @return Whether the given class is immutable.
         */
        def objectTypeIsImmutable(objectType: ObjectType): Boolean = {
            val classFile = project.classFile(objectType)
            if (classFile.isDefined) {
                classIsImmutable(classFile.get)
            } else {
                unknownClassFiles.add(objectType)
                false
            }
        }

        /*
         * Checks whether a class only has immutable fields.
         *
         * @param objectType The `ObjectType` to check.
         * @return `true` if the class only has immutable fields, `false` if the class has
         * any mutable fields.
         */
        def classOnlyHasImmutableFields(classFile: ClassFile): Boolean = {
            for (field ← classFile.fields) yield {
                if (fieldIsMutable(classFile, field).isDefined) {
                    return false
                }
            }
            true
        }

        /*
         * Checks whether a class only has fields with immutable types.
         *
         * @param project `Project` containing the class.
         * @param classFile The `ClassFile` to check.
         * @return `true` if the class only has fields with immutable type, `false` if the
         * class has any fields with mutable type.
         */
        def classOnlyHasFieldsWithImmutableTypes(objectType: ObjectType): Boolean = {
            val classFile = project.classFile(objectType)
            if (!classFile.isDefined) {
                unknownClassFiles.add(objectType)
                return false
            }

            for (field ← classFile.get.fields) {
                if (!fieldTypeIsImmutable(field.fieldType)) {
                    return false
                }
            }

            true
        }

        /*
         * Checks whether a class lacking annotations is mutable or immutable and adds it
         * to the respective container in the companion object for accelerated future
         * lookup.
         *
         * @param classFile The class to be evaluated and added to a container.
         * @return `true` if the class was deemed immutable, else `false`.
         */
        def uncachedClassIsImmutable(classFile: ClassFile): Boolean = {
            alreadySeenThisCycle.add(classFile)

            val isImmutable = isAnnotatedWith(classFile, immutableAnnotationTypes) ||
                classOnlyHasImmutableFields(classFile)

            alreadySeenThisCycle.remove(classFile)

            if (isImmutable) {
                collectedImmutables.add(classFile)
            } else {
                collectedMutables.add(classFile)
            }

            isImmutable
        }

        /*
         * Checks whether a `FieldType` is immutable.
         *
         * @param fieldType The `FieldType` to check.
         * @return `true` if the field is immutable, else `false`.
         */
        def fieldTypeIsImmutable(fieldType: FieldType): Boolean = {
            fieldType.isBaseType ||
                fieldType == ObjectType.String ||
                fieldType == ObjectType.Object ||
                !fieldType.isArrayType &&
                (
                    isPrimitiveWrapper(fieldType.asObjectType) ||
                    objectTypeIsImmutable(fieldType.asObjectType)
                )
        }

        /*
         * Checks whether a class contains a public method that sets a given field, even
         * indirectly.
         *
         * @param field The field to be searched for.
         * @param classFile The class to be checked for the public setter.
         * @return `true` if the class contains an (indirect) public setter for the field,
         * else `false`.
         */
        def hasPublicSetter(field: Field, classFile: ClassFile): Boolean = {
            val thisType = classFile.thisType
            val fieldName = field.name
            val fieldType = field.fieldType

            // Find methods containing instructions to set the field,
            // ignoring constructors and static methods.
            val directSetters = (
                for {
                    method @ MethodWithBody(body) ← classFile.methods
                    if (!method.isConstructor && !method.isStatic)
                    PUTFIELD(`thisType`, `fieldName`, `fieldType`) ← body.instructions
                } yield {
                    method
                })

            directSetters.nonEmpty && {
                var transitiveHull = directSetters
                var oldTransitiveHull: IndexedSeq[Method] = IndexedSeq.empty

                val ComputedCallGraph(callGraph, _, _) = project.get(CHACallGraphKey)

                // Build the transitive hull of the called by-relation
                while (!transitiveHull.equals(oldTransitiveHull)) {
                    oldTransitiveHull = transitiveHull
                    for (method ← oldTransitiveHull) {
                        val callers = callGraph.calledBy(method)
                        for {
                            callerMethod ← callers.keys
                            if (!transitiveHull.contains(callerMethod) &&
                                !callerMethod.isStatic &&
                                !callerMethod.isConstructor)
                        } {
                            transitiveHull = transitiveHull :+ callerMethod
                        }
                    }
                }

                // Found at least one non-private setter?
                transitiveHull.filterNot(_.isPrivate).nonEmpty
            }
        }

        /*
         * Checks whether a field is defensively copied every time before being passed
         * in or out of the class, and whether the copy is deep enough.
         *
         * @param declaringClass The class to search in.
         * @param field The field to check.
         * @return `true` if the field is correctly defensively copied, else `false`.
         */
        def fieldIsDefensivelyCopied(declaringClass: ClassFile, field: Field): Boolean = {
            val thisType = declaringClass.thisType
            val fieldName = field.name
            val fieldType = field.fieldType

            // For all methods PUTing this field
            val putMethods = (for {
                method @ MethodWithBody(body) ← declaringClass.methods
                PUTFIELD(`thisType`, `fieldName`, `fieldType`) ← body.instructions
            } yield {
                // Run AI
                val domain = new ImmutabilityAnalysisDomain((declaringClass, field))
                val results = BaseAI(declaringClass, method, domain)

                // For each PUTFIELD of this field, check whether
                // 1. the operand is a parameter. If so, it's probably not defensively
                //    copied, because the caller that passed in the reference may still
                //    hold the reference after the call, allowing him to modify the
                //    field's content (in case it's an array or class) through that
                //    reference.
                // 2. the operand is the result of a NEW/NEWARRAY/ANEWARRAY or a clone()
                //    call. If so, this could potentially be a defensive copy, if the
                //    field itself has immutable types (or is a class that consists only
                //    of fields with immutable types).
                // TODO (future improvement): Currently doing simple deepness check only.
                // This could be improved, for example to take manual defensive copying
                // into account.
                val codeWithIndex = results.code.associateWithIndex
                for {
                    (pc, PUTFIELD(`thisType`, `fieldName`, `fieldType`)) ← codeWithIndex
                    operands = results.operandsArray(pc)
                } yield {
                    val originPc = domain.origin(operands.head).head
                    originPc >= 0 && {
                        results.code.instructions(originPc) match {
                            case NEW(_) | NEWARRAY(_) | ANEWARRAY(_) |
                                INVOKEVIRTUAL(`fieldType`, "clone",
                                    MethodDescriptor(IndexedSeq(), ObjectType.Object)) ⇒
                                if (fieldType.isArrayType) {
                                    fieldTypeIsImmutable(
                                        fieldType.asArrayType.elementType)
                                } else {
                                    classOnlyHasFieldsWithImmutableTypes(
                                        fieldType.asObjectType)
                                }
                            case _ ⇒ false
                        }
                    }
                }
            }).flatten

            !putMethods.contains(false) && {
                val getMethods = (for {
                    method @ MethodWithBody(body) ← declaringClass.methods
                    GETFIELD(`thisType`, `fieldName`, `fieldType`) ← body.instructions
                } yield {
                    val domain = new ImmutabilityAnalysisDomain(declaringClass, field)
                    val results = BaseAI(declaringClass, method, domain)
                    val codeWithIndex = results.code.associateWithIndex
                    for {
                        (pc, GETFIELD(`thisType`, `fieldName`,
                            `fieldType`)) ← codeWithIndex
                        returnInstruction @ (_, ARETURN) ← codeWithIndex
                        operands = results.operandsArray(returnInstruction._1)
                    } yield {
                        domain.origin(operands.head).head != pc
                    }
                }).flatten

                !getMethods.contains(false)
            }
        }

        /*
         * Recursively checks whether a field introduces mutable behavior to a class.
         *
         * @param classFile The class containing the field.
         * @param field The field to be checked for immutability.
         * @return Short message explaining, why the field makes the class mutable.
         * Returns `None` if the field is immutable.
         */
        def fieldIsMutable(classFile: ClassFile, field: Field): Option[String] = {
            if (field.isPublic || field.isProtected) {
                if (field.isNonFinal) {
                    Some("is mutable, because it is not private, and not final.")
                } else if (!fieldTypeIsImmutable(field.fieldType)) {
                    Some("is mutable, because it is a non private final reference"+
                        " to a mutable object.")
                } else {
                    None
                }
            } else if (field.isFinal) {
                if (!fieldTypeIsImmutable(field.fieldType) &&
                    !fieldIsDefensivelyCopied(classFile, field)) {
                    Some("is mutable, because it isn't defensively copied every "+
                        "time it is passed in or out of the class, or the"+
                        " defensive copy is not deep enough.")
                } else {
                    None
                }
            } else if (hasPublicSetter(field, classFile)) {
                Some("is mutable because it has an (indirect) public setter")
            } else if (!fieldTypeIsImmutable(field.fieldType) &&
                !fieldIsDefensivelyCopied(classFile, field)) {
                Some("is mutable, because it isn't defensively copied every "+
                    "time it is passed in or out of the class, or the"+
                    " defensive copy is not deep enough.")
            } else {
                None
            }
        }

        val analysisOutput = (for {
            classFile ← project.classFiles
            if !project.isLibraryType(classFile)
            if (isAnnotatedWith(classFile, immutableAnnotationTypes))
            field ← classFile.fields
            if (!field.isStatic)
            message = fieldIsMutable(classFile, field)
            if (message.isDefined)
        } yield {
            FieldBasedReport(
                project.source(classFile.thisType),
                Severity.Warning,
                classFile.thisType,
                field,
                message.get)
        }).toSet

        val classNotFoundOutput =
            (for (classFile ← unknownClassFiles) yield {
                ClassBasedReport(
                    project.source(classFile),
                    Severity.Info,
                    classFile,
                    "There was not enough information about this class. We treat it as"+
                        " mutable.")
            }).toSet

        val cyclicCompositionOutput = (for (classFile ← immutableClassesInACycle) yield {
            ClassBasedReport(
                project.source(classFile.thisType),
                Severity.Info,
                classFile.thisType,
                "is part of a cyclic composition. We treat it as mutable.")
        }).toSet

        analysisOutput ++ classNotFoundOutput ++ cyclicCompositionOutput
    }
}
