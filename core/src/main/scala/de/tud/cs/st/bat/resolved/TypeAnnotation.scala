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
package de.tud.cs.st
package bat
package resolved

/**
 * Describes the kind of the target of a [[TypeAnnotation]].
 *
 * @author Michael Eichberg
 */
sealed trait TypeAnnotationTarget

/**
 * The path that describes which type is actually annotated using a [[TypeAnnotation]].
 *
 * @author Michael Eichberg
 */
sealed trait TypeAnnotationPath

/**
 * An element of the path that describes which type is actually annotated using
 * a [[TypeAnnotation]].
 *
 * @author Michael Eichberg
 */
sealed trait TypeAnnotationPathElement

/**
 * A type annotation (*TA*).
 *
 * [[TypeAnnotations]] were introduced with Java 8 and
 * are associated with a [[ClassFile]],
 * [[Field]], [[Method]] or [[Code]] using a
 * [[de.tud.cs.st.bat.resolved.RuntimeInvisibleTypeAnnotationTable]] or a
 * [[de.tud.cs.st.bat.resolved.RuntimeVisibleTypeAnnotationTable]] attribute.
 *
 * @author Michael Eichberg
 */
case class TypeAnnotation(
        target: TypeAnnotationTarget,
        path: TypeAnnotationPath,
        annotationType: FieldType,
        elementValuePairs: ElementValuePairs) {

}

case class TAOfCastExpression(
    offset: Int,
    type_argument_index: Int) extends TypeAnnotationTarget

case class TAOfCatch(
    exception_table_index: Int) extends TypeAnnotationTarget

case class TAOfConstructorInMethodReferenceExpression(
    offset: Int,
    type_argument_index: Int) extends TypeAnnotationTarget

case class TAOfConstructorInvocation(
    offset: Int,
    type_argument_index: Int) extends TypeAnnotationTarget

case object TAOfFieldDeclaration extends TypeAnnotationTarget

case class TAOfFormalParameter(
    formal_parameter_index: Int) extends TypeAnnotationTarget

case class TAOfInstanceOf(
    offset: Int) extends TypeAnnotationTarget

case class TAOfLocalvarDecl(
    localVarTable: IndexedSeq[LocalvarTableEntry]) extends TypeAnnotationTarget

case class TAOfResourcevarDecl(
    localVarTable: IndexedSeq[LocalvarTableEntry]) extends TypeAnnotationTarget

case class LocalvarTableEntry(
    start_pc: Int,
    length: Int,
    local_variable_table_index: Int)

case class TAOfMethodInMethodReferenceExpression(
    offset: Int,
    type_argument_index: Int) extends TypeAnnotationTarget

case class TAOfMethodInvocation(
    offset: Int,
    type_argument_index: Int) extends TypeAnnotationTarget

case class TAOfMethodReferenceExpressionIdentifier(
    offset: Int) extends TypeAnnotationTarget

case class TAOfMethodReferenceExpressionNew(
    offset: Int) extends TypeAnnotationTarget

case class TAOfNew(
    offset: Int) extends TypeAnnotationTarget

case class TAOfParameterDeclarationOfClassOrInterface(
    type_parameter_index: Int) extends TypeAnnotationTarget

case class TAOfParameterDeclarationOfMethodOrConstructor(
    type_parameter_index: Int) extends TypeAnnotationTarget

case object TAOfReceiverType extends TypeAnnotationTarget

case object TAOfReturnType extends TypeAnnotationTarget

case class TAOfSupertype(
    supertype_index: Int) extends TypeAnnotationTarget

case class TAOfThrows(
    throws_type_index: Int) extends TypeAnnotationTarget

case class TAOfTypeBoundOfParameterDeclarationOfClassOrInterface(
    type_parameter_index: Int,
    bound_index: Int) extends TypeAnnotationTarget

case class TAOfTypeBoundOfParameterDeclarationOfMethodOrConstructor(
    type_parameter_index: Int,
    bound_index: Int) extends TypeAnnotationTarget

//
//
// TypeAnnotationPath(Element)
//
//

case object TADirectlyOnType extends TypeAnnotationPath

case class TAOnNestedType(
    path: IndexedSeq[TypeAnnotationPathElement]) extends TypeAnnotationPath

case object TADeeperInArrayType extends TypeAnnotationPathElement

case object TADeeperInNestedType extends TypeAnnotationPathElement

case object TAOnBoundOfWildcardType extends TypeAnnotationPathElement

case class TAOnTypeArgument(
    type_argument_index: Int) extends TypeAnnotationPathElement



