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
package br

/**
 * Describes the kind of the target of a [[TypeAnnotation]].
 *
 * @author Michael Eichberg
 */
sealed abstract class TypeAnnotationTarget {

    def typeId: Int
}

/**
 * The path that describes which type is actually annotated using a [[TypeAnnotation]].
 *
 * @author Michael Eichberg
 */
sealed abstract class TypeAnnotationPath

/**
 * An element of the path that describes which type is actually annotated using
 * a [[TypeAnnotation]].
 *
 * @author Michael Eichberg
 */
sealed abstract class TypeAnnotationPathElement {
    /**
     * A value in the range [0..3] which identifies the `path kind` as specified by the JVM
     * specification.
     *
     * @note This enables efficient identificaion – e.g., in a switch – of the type path kind.
     */
    def kindId: Int
}

/**
 * A type annotation (*TA*).
 *
 * [[TypeAnnotations]] were introduced with Java 8 and
 * are associated with a [[ClassFile]],
 * [[Field]], [[Method]] or [[Code]] using a
 * [[org.opalj.br.RuntimeInvisibleTypeAnnotationTable]] or a
 * [[org.opalj.br.RuntimeVisibleTypeAnnotationTable]] attribute.
 *
 * @author Michael Eichberg
 */
case class TypeAnnotation(
        target:            TypeAnnotationTarget,
        path:              TypeAnnotationPath,
        annotationType:    FieldType,
        elementValuePairs: ElementValuePairs
)

case class TAOfCastExpression(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x47
}

case class TAOfCatch(exception_table_index: Int) extends TypeAnnotationTarget {
    def typeId: Int = 0x42
}

case class TAOfConstructorInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x4A
}

case class TAOfConstructorInvocation(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x48
}

case object TAOfFieldDeclaration extends TypeAnnotationTarget {
    def typeId: Int = 0x13
}

case class TAOfFormalParameter(formal_parameter_index: Int) extends TypeAnnotationTarget {
    def typeId: Int = 0x16
}

case class TAOfInstanceOf(offset: Int) extends TypeAnnotationTarget {
    def typeId: Int = 0x43
}

case class TAOfLocalvarDecl(
        localVarTable: IndexedSeq[LocalvarTableEntry]
) extends TypeAnnotationTarget {
    def typeId: Int = 0x40
}

case class TAOfResourcevarDecl(
        localVarTable: IndexedSeq[LocalvarTableEntry]
) extends TypeAnnotationTarget {
    def typeId: Int = 0x41
}

case class LocalvarTableEntry(
        startPC: Int,
        length:  Int,
        index:   Int
)

case class TAOfMethodInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x4B
}

case class TAOfMethodInvocation(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x49
}

case class TAOfMethodReferenceExpressionIdentifier(offset: Int) extends TypeAnnotationTarget {
    def typeId: Int = 0x46
}

case class TAOfMethodReferenceExpressionNew(offset: Int) extends TypeAnnotationTarget {
    def typeId: Int = 0x45
}

case class TAOfNew(offset: Int) extends TypeAnnotationTarget {
    def typeId: Int = 0x44
}

case class TAOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x00
}

case class TAOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x01
}

case object TAOfReceiverType extends TypeAnnotationTarget {
    def typeId: Int = 0x15
}

case object TAOfReturnType extends TypeAnnotationTarget {
    def typeId: Int = 0x14
}

case class TAOfSupertype(supertype_index: Int) extends TypeAnnotationTarget {
    def typeId: Int = 0x10
}

case class TAOfThrows(throws_type_index: Int) extends TypeAnnotationTarget {
    def typeId: Int = 0x17
}

case class TAOfTypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index:          Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x11
}

case class TAOfTypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index:          Int
) extends TypeAnnotationTarget {
    def typeId: Int = 0x12
}

//
//
// TypeAnnotationPath(Element)
//
//

// path length == 0
case object TADirectlyOnType extends TypeAnnotationPath

// path length > 0
case class TAOnNestedType(path: IndexedSeq[TypeAnnotationPathElement]) extends TypeAnnotationPath

case object TADeeperInArrayType extends TypeAnnotationPathElement {
    final override def kindId: Int = KindId
    final val KindId = 0
}

case object TADeeperInNestedType extends TypeAnnotationPathElement {
    final override def kindId: Int = KindId
    final val KindId = 1
}

case object TAOnBoundOfWildcardType extends TypeAnnotationPathElement {
    final override def kindId: Int = KindId
    final val KindId = 2
}

case class TAOnTypeArgument(index: Int) extends TypeAnnotationPathElement {
    final override def kindId: Int = TAOnTypeArgument.KindId
}
object TAOnTypeArgument {
    final val KindId = 3
}
