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

import org.opalj.collection.immutable.UIDSet

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

    /**
     * An upper type bound represents the available type information about a
     * reference value. It is always "just" an upper bound for a concrete type;
     * i.e., we know that the runtime type has to be a subtype (reflexive) of the
     * type identified by the upper bound.
     * Furthermore, an upper bound can identify multiple '''independent''' types. E.g.,
     * a type bound for array objects could be: `java.io.Serializable` and
     * `java.lang.Cloneable`. Here, independent means that no two types of the bound
     * are in a subtype relationship. Hence, an upper bound is always a special set where
     * the values are not equal and are not in an inheritance relation. However,
     * identifying independent types is a class hierarchy's responsibility.
     *
     * In general, an upper bound identifies a single class type and a set of independent
     * interface types that are known to be implemented by the current object. '''Even if
     * the type contains a class type''' it may just be a super class of the concrete type
     * and, hence, just represent an abstraction.
     */
    type UpperTypeBound = UIDSet[ReferenceType]

}