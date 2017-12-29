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

    /**
     * Remaps the pcs/offset by calling the given function; this is particularly required
     * when the bytecode is optimized/transformed/instrumented to ensure the annotations are
     * still valid.
     */
    def remapPCs(f: PC ⇒ PC): TypeAnnotationTarget

}

sealed abstract class TypeAnnotationTargetInCode extends TypeAnnotationTarget

sealed abstract class TypeAnnotationTargetInClassDeclaration extends TypeAnnotationTarget {
    final override def remapPCs(f: PC ⇒ PC): TypeAnnotationTarget = this
}

sealed abstract class TypeAnnotationTargetInFieldDeclaration extends TypeAnnotationTarget {
    final override def remapPCs(f: PC ⇒ PC): TypeAnnotationTarget = this
}

sealed abstract class TypeAnnotationTargetInMetodDeclaration extends TypeAnnotationTarget {
    final override def remapPCs(f: PC ⇒ PC): TypeAnnotationTarget = this
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
) extends AnnotationLike {
    def remapPCs(f: PC ⇒ PC): TypeAnnotation = copy(target = target.remapPCs(f))
}

//
//
// The TypeAnnotationTargets
//
//

case class TAOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int
) extends TypeAnnotationTargetInClassDeclaration {
    def typeId: Int = 0x00
}

case class TAOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int
) extends TypeAnnotationTargetInMetodDeclaration {
    def typeId: Int = 0x01
}

case class TAOfSupertype(supertype_index: Int) extends TypeAnnotationTargetInClassDeclaration {
    def typeId: Int = 0x10
}

case class TAOfTypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index:          Int
) extends TypeAnnotationTargetInClassDeclaration {
    def typeId: Int = 0x11
}

case class TAOfTypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index:          Int
) extends TypeAnnotationTargetInMetodDeclaration {
    def typeId: Int = 0x12
}

case object TAOfFieldDeclaration extends TypeAnnotationTargetInFieldDeclaration {
    def typeId: Int = 0x13
}

case object TAOfReturnType extends TypeAnnotationTargetInMetodDeclaration {
    def typeId: Int = 0x14
}

case object TAOfReceiverType extends TypeAnnotationTargetInMetodDeclaration {
    def typeId: Int = 0x15
}

case class TAOfFormalParameter(
        formal_parameter_index: Int
) extends TypeAnnotationTargetInMetodDeclaration {
    def typeId: Int = 0x16
}

case class TAOfThrows(throws_type_index: Int) extends TypeAnnotationTargetInMetodDeclaration {
    def typeId: Int = 0x17
}

sealed abstract class TypeAnnotationTargetInVarDecl extends TypeAnnotationTargetInCode {
    def localVarTable: IndexedSeq[LocalvarTableEntry]
}

case class TAOfLocalvarDecl(
        localVarTable: IndexedSeq[LocalvarTableEntry]
) extends TypeAnnotationTargetInVarDecl {
    override def typeId: Int = 0x40
    override def remapPCs(f: PC ⇒ PC): TAOfLocalvarDecl = {
        copy(localVarTable.map(_.remapPCs(f)))
    }
}

case class TAOfResourcevarDecl(
        localVarTable: IndexedSeq[LocalvarTableEntry]
) extends TypeAnnotationTargetInVarDecl {
    override def typeId: Int = 0x41
    override def remapPCs(f: PC ⇒ PC): TAOfResourcevarDecl = {
        copy(localVarTable.map(_.remapPCs(f)))
    }
}

/** The local variable is valid in the range `[start_pc, start_pc + length)`. */
case class LocalvarTableEntry(
        startPC: Int,
        length:  Int,
        index:   Int
) {
    def remapPCs(f: PC ⇒ PC): LocalvarTableEntry = {
        val newStartPC = f(startPC)
        LocalvarTableEntry(
            newStartPC,
            f(startPC + length) - newStartPC,
            index
        )
    }
}

case class TAOfCatch(exception_table_index: Int) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x42
    override def remapPCs(f: PC ⇒ PC): this.type = this
}

case class TAOfInstanceOf(offset: Int) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x43
    override def remapPCs(f: PC ⇒ PC): TAOfInstanceOf = new TAOfInstanceOf(f(offset))
}

case class TAOfNew(offset: Int) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x44
    override def remapPCs(f: PC ⇒ PC): TAOfNew = new TAOfNew(f(offset))
}

case class TAOfMethodReferenceExpressionNew(offset: Int) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x45
    override def remapPCs(f: PC ⇒ PC): TAOfMethodReferenceExpressionNew = {
        new TAOfMethodReferenceExpressionNew(f(offset))
    }
}

case class TAOfMethodReferenceExpressionIdentifier(offset: Int) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x46
    override def remapPCs(f: PC ⇒ PC): TAOfMethodReferenceExpressionIdentifier = {
        new TAOfMethodReferenceExpressionIdentifier(f(offset))
    }
}

case class TAOfCastExpression(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x47
    override def remapPCs(f: PC ⇒ PC): TAOfCastExpression = copy(offset = f(offset))
}

case class TAOfConstructorInvocation(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x48
    override def remapPCs(f: PC ⇒ PC): TAOfConstructorInvocation = copy(offset = f(offset))
}

case class TAOfMethodInvocation(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x49
    override def remapPCs(f: PC ⇒ PC): TAOfMethodInvocation = copy(offset = f(offset))
}

case class TAOfConstructorInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x4A
    override def remapPCs(f: PC ⇒ PC): TAOfConstructorInMethodReferenceExpression = {
        copy(offset = f(offset))
    }
}

case class TAOfMethodInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
) extends TypeAnnotationTargetInCode {
    def typeId: Int = 0x4B
    override def remapPCs(f: PC ⇒ PC): TAOfMethodInMethodReferenceExpression = {
        copy(offset = f(offset))
    }
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
