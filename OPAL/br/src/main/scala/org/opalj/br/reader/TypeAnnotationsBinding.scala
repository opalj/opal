/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.TypeAnnotationsReader
import org.opalj.bi.reader.TypeAnnotationTargetReader
import org.opalj.bi.reader.TypeAnnotationPathReader

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

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
    override implicit val typeAnnotationType: ClassTag[TypeAnnotation] = ClassTag(classOf[br.TypeAnnotation])

    type TypeAnnotationTarget = br.TypeAnnotationTarget

    type TypeAnnotationPath = br.TypeAnnotationPath

    type TypeAnnotationPathElement = br.TypeAnnotationPathElement
    override implicit val typeAnnotationPathElementType: ClassTag[TypeAnnotationPathElement] = ClassTag(classOf[br.TypeAnnotationPathElement])

    type LocalvarTableEntry = br.LocalvarTableEntry
    override implicit val localvarTableEntryType: ClassTag[LocalvarTableEntry] = ClassTag(classOf[br.LocalvarTableEntry])

    //
    // TypeAnnotation
    //

    override def TypeAnnotation(
        cp:                  Constant_Pool,
        target:              TypeAnnotationTarget,
        path:                TypeAnnotationPath,
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs
    ): TypeAnnotation = {
        new TypeAnnotation(target, path, cp(type_index).asFieldType, element_value_pairs)
    }

    //
    // TypeAnnotationTarget
    //

    //______________________________
    // type_parameter_target
    override def ParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int
    ): TAOfParameterDeclarationOfClassOrInterface = {
        TAOfParameterDeclarationOfClassOrInterface(type_parameter_index)
    }

    override def ParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int
    ): TAOfParameterDeclarationOfMethodOrConstructor = {
        TAOfParameterDeclarationOfMethodOrConstructor(type_parameter_index)
    }

    //______________________________
    // supertype_target
    override def SupertypeTarget(
        supertype_index: Int
    ): TAOfSupertype = {
        TAOfSupertype(supertype_index)
    }
    //______________________________
    // type_parameter_bound_target
    override def TypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index:          Int
    ): TAOfTypeBoundOfParameterDeclarationOfClassOrInterface = {
        TAOfTypeBoundOfParameterDeclarationOfClassOrInterface(
            type_parameter_index,
            bound_index
        )
    }

    override def TypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index:          Int
    ): TAOfTypeBoundOfParameterDeclarationOfMethodOrConstructor = {
        TAOfTypeBoundOfParameterDeclarationOfMethodOrConstructor(
            type_parameter_index,
            bound_index
        )
    }

    //______________________________
    // empty_target
    override def FieldDeclaration: TAOfFieldDeclaration.type = TAOfFieldDeclaration

    override def ReturnType: TAOfReturnType.type = TAOfReturnType

    override def ReceiverType: TAOfReceiverType.type = TAOfReceiverType

    //______________________________
    // formal_parameter_target
    override def FormalParameter(formal_parameter_index: Int): TAOfFormalParameter = {
        TAOfFormalParameter(formal_parameter_index)
    }

    //______________________________
    // throws_target
    override def Throws(throws_type_index: Int): TAOfThrows = TAOfThrows(throws_type_index)

    //______________________________
    // catch_target
    override def Catch(exception_table_index: Int): TAOfCatch = TAOfCatch(exception_table_index)

    //______________________________
    // localvar_target

    override def LocalvarTableEntry(
        start_pc:                   Int,
        length:                     Int,
        local_variable_table_index: Int
    ): LocalvarTableEntry = {
        new LocalvarTableEntry(start_pc, length, local_variable_table_index)
    }

    override def LocalvarDecl(localVarTable: LocalvarTable): TAOfLocalvarDecl = {
        TAOfLocalvarDecl(localVarTable)
    }

    override def ResourcevarDecl(localVarTable: LocalvarTable): TAOfResourcevarDecl = {
        TAOfResourcevarDecl(localVarTable)
    }

    //______________________________
    // offset_target
    override def InstanceOf(offset: Int): TAOfInstanceOf = TAOfInstanceOf(offset)

    override def New(offset: Int): TAOfNew = TAOfNew(offset)

    override def MethodReferenceExpressionNew /*::New*/ (
        offset: Int
    ): TAOfMethodReferenceExpressionNew = {
        TAOfMethodReferenceExpressionNew(offset)
    }

    override def MethodReferenceExpressionIdentifier /*::Identifier*/ (
        offset: Int
    ): TAOfMethodReferenceExpressionIdentifier = {
        TAOfMethodReferenceExpressionIdentifier(offset)
    }

    //______________________________
    // type_arguement_target
    override def CastExpression(
        offset:              Int,
        type_argument_index: Int
    ): TAOfCastExpression = {
        TAOfCastExpression(offset, type_argument_index)
    }

    override def ConstructorInvocation(
        offset:              Int,
        type_argument_index: Int
    ): TAOfConstructorInvocation = {
        TAOfConstructorInvocation(offset, type_argument_index)
    }

    override def MethodInvocation(
        offset:              Int,
        type_argument_index: Int
    ): TAOfMethodInvocation = {
        TAOfMethodInvocation(offset, type_argument_index)
    }

    override def ConstructorInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
    ): TAOfConstructorInMethodReferenceExpression = {
        TAOfConstructorInMethodReferenceExpression(
            offset,
            type_argument_index
        )
    }
    override def MethodInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
    ): TAOfMethodInMethodReferenceExpression = {
        TAOfMethodInMethodReferenceExpression(offset, type_argument_index)
    }

    //
    // TypeAnnotationPath
    //

    override def TypeAnnotationDirectlyOnType: TADirectlyOnType.type =
        TADirectlyOnType

    override def TypeAnnotationPath(
        path: ArraySeq[TypeAnnotationPathElement]
    ): TAOnNestedType = {
        TAOnNestedType(path)
    }

    override def TypeAnnotationDeeperInArrayType: TADeeperInArrayType.type = {
        TADeeperInArrayType
    }

    override def TypeAnnotationDeeperInNestedType: TADeeperInNestedType.type = {
        TADeeperInNestedType
    }

    override def TypeAnnotationOnBoundOfWildcardType: TAOnBoundOfWildcardType.type = {
        TAOnBoundOfWildcardType
    }

    override def TypeAnnotationOnTypeArgument(type_argument_index: Int): TAOnTypeArgument = {
        TAOnTypeArgument(type_argument_index)
    }

}

