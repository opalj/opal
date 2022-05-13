/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.annotation.switch
import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Generic parser for the `target_type` and `target_info` fields of type annotations.
 * This reader is intended to be used in conjunction with the
 * [[TypeAnnotationsReader]].
 */
trait TypeAnnotationTargetReader extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type TypeAnnotationTarget <: AnyRef

    //______________________________
    // type_parameter_target
    def ParameterDeclarationOfClassOrInterface(type_parameter_index: Int): TypeAnnotationTarget
    def ParameterDeclarationOfMethodOrConstructor(type_parameter_index: Int): TypeAnnotationTarget

    //______________________________
    // supertype_target
    def SupertypeTarget(supertype_index: Int): TypeAnnotationTarget

    //______________________________
    // type_parameter_bound_target
    def TypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int,
        bound_index:          Int
    ): TypeAnnotationTarget
    def TypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Int,
        bound_index:          Int
    ): TypeAnnotationTarget

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
    /*
     * Format
     * {{{
     * u2 table_length;
     * {    u2 start_pc;
     *      u2 length;
     *      u2 index; // index into the local variable table(!)
     * } table[table_length];
     * }}}
     */
    type LocalvarTableEntry <: AnyRef
    implicit val localvarTableEntryType: ClassTag[LocalvarTableEntry] // TODO: Replace in Scala 3 by `type LocalvarTableEntry : ClassTag`
    type LocalvarTable = ArraySeq[LocalvarTableEntry]
    /**
     * Factory method to create a `LocalvarTableEntry`. To completely resolve
     * such entries; i.e., to resolve the local_variable_table_index it may
     * be necessary to do some post-processing after all attributes belonging
     * to a code block are loaded. This can be done using the method
     * [[org.opalj.bi.reader.AttributeReader.registerAttributesPostProcessor]].
     */
    def LocalvarTableEntry(
        start_pc:                   Int,
        length:                     Int,
        local_variable_table_index: Int
    ): LocalvarTableEntry
    def LocalvarDecl(localVarTable: LocalvarTable): TypeAnnotationTarget
    def ResourcevarDecl(localVarTable: LocalvarTable): TypeAnnotationTarget

    //______________________________
    // offset_target
    def InstanceOf(offset: Int): TypeAnnotationTarget
    def New(offset: Int): TypeAnnotationTarget
    def MethodReferenceExpressionNew /*::New*/ (
        offset: Int
    ): TypeAnnotationTarget
    def MethodReferenceExpressionIdentifier /*::Identifier*/ (
        offset: Int
    ): TypeAnnotationTarget

    //______________________________
    // type_argument_target
    def CastExpression(
        offset:              Int,
        type_argument_index: Int
    ): TypeAnnotationTarget
    def ConstructorInvocation(
        offset:              Int,
        type_argument_index: Int
    ): TypeAnnotationTarget
    def MethodInvocation(
        offset:              Int,
        type_argument_index: Int
    ): TypeAnnotationTarget
    def ConstructorInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
    ): TypeAnnotationTarget
    def MethodInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
    ): TypeAnnotationTarget

    //
    // IMPLEMENTATION
    //

    def LocalvarTable(in: DataInputStream): LocalvarTable = {
        fillArraySeq(in.readUnsignedShort) {
            LocalvarTableEntry(
                in.readUnsignedShort(),
                in.readUnsignedShort(),
                in.readUnsignedShort()
            )
        }
    }

    /**
     * <pre>
     * u1 target_type;
     * union {
     *  type_parameter_target;
     *  supertype_target;
     *  type_parameter_bound_target;
     *  empty_target;
     *  method_formal_parameter_target;
     *  throws_target;
     *  localvar_target;
     *  catch_target;
     *  offset_target;
     *  type_argument_target;
     *  } target_info;
     * </pre>
     */
    def TypeAnnotationTarget(in: DataInputStream): TypeAnnotationTarget = {
        val target_type = in.readUnsignedByte()
        (target_type: @switch) match {
            case 0x00 => ParameterDeclarationOfClassOrInterface(in.readUnsignedByte())
            case 0x01 => ParameterDeclarationOfMethodOrConstructor(in.readUnsignedByte())
            case 0x10 => SupertypeTarget(in.readUnsignedShort())
            case 0x11 =>
                TypeBoundOfParameterDeclarationOfClassOrInterface(
                    in.readUnsignedByte(),
                    in.readUnsignedByte()
                )
            case 0x12 =>
                TypeBoundOfParameterDeclarationOfMethodOrConstructor(
                    in.readUnsignedByte(),
                    in.readUnsignedByte()
                )
            case 0x13 => FieldDeclaration
            case 0x14 => ReturnType
            case 0x15 => ReceiverType
            case 0x16 => FormalParameter(in.readUnsignedByte())
            case 0x17 => Throws(in.readUnsignedShort())
            case 0x40 => LocalvarDecl(LocalvarTable(in))
            case 0x41 => ResourcevarDecl(LocalvarTable(in))
            case 0x42 => Catch(in.readUnsignedShort())
            case 0x43 => InstanceOf(in.readUnsignedShort())
            case 0x44 => New(in.readUnsignedShort())
            case 0x45 => MethodReferenceExpressionNew(in.readUnsignedShort())
            case 0x46 => MethodReferenceExpressionIdentifier(in.readUnsignedShort())
            case 0x47 => CastExpression(in.readUnsignedShort(), in.readUnsignedByte())
            case 0x48 => ConstructorInvocation(in.readUnsignedShort(), in.readUnsignedByte())
            case 0x49 => MethodInvocation(in.readUnsignedShort(), in.readUnsignedByte())
            case 0x4A =>
                ConstructorInMethodReferenceExpression(
                    in.readUnsignedShort(),
                    in.readUnsignedByte()
                )
            case 0x4B =>
                MethodInMethodReferenceExpression(in.readUnsignedShort(), in.readUnsignedByte())
        }
    }
}
