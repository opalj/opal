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
package reader

import reflect.ClassTag

import de.tud.cs.st.bat.reader.TypeAnnotationsReader
import de.tud.cs.st.bat.reader.TypeAnnotationTargetReader
import de.tud.cs.st.bat.reader.TypeAnnotationPathReader

/**
 * Factory methods to create representations of Java annotations.
 *
 * @author Michael Eichberg
 */
trait TypeAnnotationsBinding
        extends TypeAnnotationsReader
        with TypeAnnotationTargetReader
        with TypeAnnotationPathReader
        with ConstantPoolBinding
        with AttributeBinding {

    type TypeAnnotation = resolved.TypeAnnotation

    val TypeAnnotationManifest: ClassTag[TypeAnnotation] = implicitly

    type TypeAnnotationTarget

    type TypeAnnotationPath

    type TypeAnnotationPathElement

    //
    // TypeAnnotation
    // 

    def TypeAnnotation(
        constant_pool: Constant_Pool,
        target: TypeAnnotationTarget,
        path: TypeAnnotationPath,
        type_index: Constant_Pool_Index,
        element_value_pairs: ElementValuePairs): TypeAnnotation

    //
    // TypeAnnotationTarget
    //

    //______________________________
    // type_parameter_target
    def ParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int): TypeAnnotationTarget
    def ParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int): TypeAnnotationTarget

    //______________________________
    // supertype_target
    def SupertypeTarget(
        supertype_index: Int): TypeAnnotationTarget

    //______________________________
    // type_parameter_bound_target
    def TypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index: Int): TypeAnnotationTarget
    def TypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index: Int): TypeAnnotationTarget

    //______________________________
    // empty_target
    def FieldDeclaration: TypeAnnotationTarget
    def ReturnType: TypeAnnotationTarget
    def ReceiverType: TypeAnnotationTarget

    //______________________________
    // formal_parameter_target
    def FormalParameter(formal_parameter_index: Int): TypeAnnotationTarget

    //______________________________
    // throws_target
    def Throws(throws_type_index: Int): TypeAnnotationTarget

    //______________________________
    // catch_target
    def Catch(exception_table_index: Int): TypeAnnotationTarget

    //______________________________
    // localvar_target
    type LocalvarTableEntry
    def LocalvarTableEntry(
        start_pc: Int,
        length: Int,
        local_variable_table_index: Int): LocalvarTableEntry
    def LocalvarDecl(localVarTable: LocalvarTable): TypeAnnotationTarget
    def ResourcevarDecl(localVarTable: LocalvarTable): TypeAnnotationTarget

    //______________________________
    // offset_target
    def InstanceOf(offset: Int): TypeAnnotationTarget
    def New(offset: Int): TypeAnnotationTarget
    def MethodReferenceExpressionNew /*::New*/ (
        offset: Int): TypeAnnotationTarget
    def MethodReferenceExpressionIdentifier /*::Identifier*/ (
        offset: Int): TypeAnnotationTarget

    //______________________________
    // type_arguement_target
    def CastExpression(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget
    def ConstructorInvocation(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget
    def MethodInvocation(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget
    def ConstructorInMethodReferenceExpression(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget
    def MethodInMethodReferenceExpression(
        offset: Int,
        type_argument_index: Int): TypeAnnotationTarget

    //
    // TypeAnnotationPath
    //

    def TypeAnnotationDirectlyOnType: TypeAnnotationPath

    def TypeAnnotationPath(path: IndexedSeq[TypeAnnotationPathElement]): TypeAnnotationPath

    def TypeAnnotationDeeperInArrayType: TypeAnnotationPathElement

    def TypeAnnotationDeeperInNestedType: TypeAnnotationPathElement

    def TypeAnnotationOnBoundOfWildcardType: TypeAnnotationPathElement

    def TypeAnnotationOnTypeArgument(type_argument_index: Int): TypeAnnotationPathElement

}



