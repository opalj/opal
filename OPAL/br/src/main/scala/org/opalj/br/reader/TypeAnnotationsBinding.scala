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
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.TypeAnnotationsReader
import org.opalj.bi.reader.TypeAnnotationTargetReader
import org.opalj.bi.reader.TypeAnnotationPathReader

/**
 * Factory methods to create representations of Java type annotations.
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationsBinding
        extends TypeAnnotationsReader
        with TypeAnnotationTargetReader
        with TypeAnnotationPathReader
        with AnnotationsBinding
        with AttributeBinding {

    type TypeAnnotation = br.TypeAnnotation

    val TypeAnnotationManifest: ClassTag[TypeAnnotation] = implicitly

    type TypeAnnotationTarget = br.TypeAnnotationTarget

    type TypeAnnotationPath = br.TypeAnnotationPath

    type TypeAnnotationPathElement = br.TypeAnnotationPathElement

    type LocalvarTableEntry = br.LocalvarTableEntry

    //
    // TypeAnnotation
    // 

    override def TypeAnnotation(
        cp: Constant_Pool,
        target: TypeAnnotationTarget,
        path: TypeAnnotationPath,
        type_index: Constant_Pool_Index,
        element_value_pairs: ElementValuePairs): TypeAnnotation = {
        new TypeAnnotation(target, path, cp(type_index).asFieldType, element_value_pairs)
    }

    //
    // TypeAnnotationTarget
    //

    //______________________________
    // type_parameter_target
    override def ParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int): TAOfParameterDeclarationOfClassOrInterface =
        TAOfParameterDeclarationOfClassOrInterface(type_parameter_index)

    override def ParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int): TAOfParameterDeclarationOfMethodOrConstructor =
        TAOfParameterDeclarationOfMethodOrConstructor(type_parameter_index)

    //______________________________
    // supertype_target
    override def SupertypeTarget(
        supertype_index: Int): TAOfSupertype =
        TAOfSupertype(supertype_index)

    //______________________________
    // type_parameter_bound_target
    override def TypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index: Int): TAOfTypeBoundOfParameterDeclarationOfClassOrInterface =
        TAOfTypeBoundOfParameterDeclarationOfClassOrInterface(
            type_parameter_index,
            bound_index)

    override def TypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index: Int): TAOfTypeBoundOfParameterDeclarationOfMethodOrConstructor =
        TAOfTypeBoundOfParameterDeclarationOfMethodOrConstructor(
            type_parameter_index,
            bound_index)

    //______________________________
    // empty_target
    override def FieldDeclaration: TAOfFieldDeclaration.type = TAOfFieldDeclaration

    override def ReturnType: TAOfReturnType.type = TAOfReturnType

    override def ReceiverType: TAOfReceiverType.type = TAOfReceiverType

    //______________________________
    // formal_parameter_target
    override def FormalParameter(formal_parameter_index: Int): TAOfFormalParameter =
        TAOfFormalParameter(formal_parameter_index)

    //______________________________
    // throws_target
    override def Throws(throws_type_index: Int): TAOfThrows =
        TAOfThrows(throws_type_index)

    //______________________________
    // catch_target
    override def Catch(exception_table_index: Int): TAOfCatch =
        TAOfCatch(exception_table_index)

    //______________________________
    // localvar_target

    override def LocalvarTableEntry(
        start_pc: Int,
        length: Int,
        local_variable_table_index: Int): LocalvarTableEntry = {
        new LocalvarTableEntry(start_pc, length, local_variable_table_index)
    }

    override def LocalvarDecl(localVarTable: LocalvarTable): TAOfLocalvarDecl =
        TAOfLocalvarDecl(localVarTable)

    override def ResourcevarDecl(localVarTable: LocalvarTable): TAOfResourcevarDecl =
        TAOfResourcevarDecl(localVarTable)

    //______________________________
    // offset_target
    override def InstanceOf(offset: Int): TAOfInstanceOf = TAOfInstanceOf(offset)

    override def New(offset: Int): TAOfNew = TAOfNew(offset)

    override def MethodReferenceExpressionNew /*::New*/ (
        offset: Int): TAOfMethodReferenceExpressionNew =
        TAOfMethodReferenceExpressionNew(offset)

    override def MethodReferenceExpressionIdentifier /*::Identifier*/ (
        offset: Int): TAOfMethodReferenceExpressionIdentifier =
        TAOfMethodReferenceExpressionIdentifier(offset)

    //______________________________
    // type_arguement_target
    override def CastExpression(
        offset: Int,
        type_argument_index: Int): TAOfCastExpression =
        TAOfCastExpression(offset, type_argument_index)

    override def ConstructorInvocation(
        offset: Int,
        type_argument_index: Int): TAOfConstructorInvocation =
        TAOfConstructorInvocation(offset, type_argument_index)

    override def MethodInvocation(
        offset: Int,
        type_argument_index: Int): TAOfMethodInvocation =
        TAOfMethodInvocation(offset, type_argument_index)

    override def ConstructorInMethodReferenceExpression(
        offset: Int,
        type_argument_index: Int): TAOfConstructorInMethodReferenceExpression =
        TAOfConstructorInMethodReferenceExpression(
            offset,
            type_argument_index)

    override def MethodInMethodReferenceExpression(
        offset: Int,
        type_argument_index: Int): TAOfMethodInMethodReferenceExpression =
        TAOfMethodInMethodReferenceExpression(offset, type_argument_index)

    //
    // TypeAnnotationPath
    //

    override def TypeAnnotationDirectlyOnType: TADirectlyOnType.type =
        TADirectlyOnType

    override def TypeAnnotationPath(
        path: IndexedSeq[TypeAnnotationPathElement]): TAOnNestedType =
        TAOnNestedType(path)

    override def TypeAnnotationDeeperInArrayType: TADeeperInArrayType.type =
        TADeeperInArrayType

    override def TypeAnnotationDeeperInNestedType: TADeeperInNestedType.type =
        TADeeperInNestedType

    override def TypeAnnotationOnBoundOfWildcardType: TAOnBoundOfWildcardType.type =
        TAOnBoundOfWildcardType

    override def TypeAnnotationOnTypeArgument(
        type_argument_index: Int): TAOnTypeArgument =
        TAOnTypeArgument(type_argument_index)

}




