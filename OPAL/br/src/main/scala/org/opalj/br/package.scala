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

/**
 * In this representation of Java bytecode references to a Java class file's constant
 * pool and to attributes are replaced by direct references to the corresponding constant
 * pool entries. This facilitates developing analyses and fosters comprehension.
 *
 * Based on the fact that indirect
 * reference to constant pool entries are resolved and replaced by direct reference this
 * representation is called the resolved representation.
 *
 * This representation of Java bytecode is considered as OPAL's standard representation
 * for writing Scala based analyses. This representation is engineered such
 * that it facilitates writing analyses that use pattern matching.
 *
 * @author Michael Eichberg
 */
package object br {

    type Attributes = Seq[Attribute]

    type ElementValuePairs = IndexedSeq[ElementValuePair]
    type Annotations = IndexedSeq[Annotation]
    type TypeAnnotations = IndexedSeq[TypeAnnotation]

    type InnerClasses = IndexedSeq[InnerClass]

    type Methods = IndexedSeq[Method]
    type Exceptions = Seq[ObjectType]
    type ExceptionHandlers = IndexedSeq[ExceptionHandler]
    type LineNumbers = Seq[LineNumber]
    type LocalVariableTypes = IndexedSeq[LocalVariableType]
    type LocalVariables = IndexedSeq[LocalVariable]
    type BootstrapMethods = IndexedSeq[BootstrapMethod]
    type BootstrapArguments = IndexedSeq[BootstrapArgument]
    type ParameterAnnotations = IndexedSeq[Annotations]
    type StackMapFrames = IndexedSeq[StackMapFrame]
    type VerificationTypeInfoLocals = IndexedSeq[VerificationTypeInfo]
    type VerificationTypeInfoStack = IndexedSeq[VerificationTypeInfo]
    type MethodParameters = IndexedSeq[MethodParameter]

    type Fields = IndexedSeq[Field]

    type Instructions = Array[instructions.Instruction]

    type SourceElementID = Int

    type Opcode = Int

    /**
     * Converts a given list of annotations into a Java-like representation.
     */
    def annotationsToJava(
        annotations: Annotations,
        before: String = "",
        after: String = ""): String = {

        def annotationToJava(annotation: Annotation): String = {
            val s = annotation.toJava
            if (s.length() > 50 && annotation.elementValuePairs.nonEmpty)
                annotation.annotationType.toJava+"(...)"
            else
                s
        }

        if (annotations.nonEmpty) {
            before + annotations.map(annotationToJava(_)).mkString(" ") + after
        } else {
            ""
        }
    }

}