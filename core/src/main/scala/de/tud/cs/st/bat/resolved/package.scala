/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat

/**
 * In this representation of Java bytecode references to a Java class file's constant
 * pool and to attributes are replaced by direct references to the corresponding constant
 * pool entries. This facilitates developing analyses and fosters comprehension.
 *
 * Based on the fact that indirect
 * reference to constant pool entries are resolved and replaced by direct reference this
 * representation is called the resolved representation.
 *
 * This representation of Java bytecode is considered as BAT's standard representation
 * for writing Scala based analyses. This representation is engineered such
 * that it facilitates writing analyses that use pattern matching.
 *
 * @author Michael Eichberg
 */
package object resolved {

    type Attributes = Seq[Attribute]

    type Annotations = IndexedSeq[Annotation]
    type ElementValuePairs = IndexedSeq[ElementValuePair]

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

    type Fields = IndexedSeq[Field]

    type Instructions = Array[Instruction]

    type SourceElementID = Int

    @throws[AnalysisFailedException]
    final def CodeError(message: String, code: Code, pc: Int) =
        throw AnalysisFailedException(generalBATExceptionMessage + message, code, pc)

}