/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

trait VerificationTypeInfoReader extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type VerificationTypeInfo

    def TopVariableInfo(): VerificationTypeInfo

    def IntegerVariableInfo(): VerificationTypeInfo

    def FloatVariableInfo(): VerificationTypeInfo

    def LongVariableInfo(): VerificationTypeInfo

    def DoubleVariableInfo(): VerificationTypeInfo

    def NullVariableInfo(): VerificationTypeInfo

    def UninitializedThisVariableInfo(): VerificationTypeInfo

    /**
     * The Uninitialized_variable_info indicates that the location contains the
     * verification type uninitialized(offset). The offset item indicates the offset of
     * the new instruction that created the object being stored in the location.
     */
    def UninitializedVariableInfo(offset: Int): VerificationTypeInfo

    /**
     * The Object_variable_info type indicates that the location contains an instance
     * of the class referenced by the constant pool entry.
     */
    def ObjectVariableInfo(cp: Constant_Pool, type_index: Constant_Pool_Index): VerificationTypeInfo

    //
    // IMPLEMENTATION
    //

    def VerificationTypeInfo(cp: Constant_Pool, in: DataInputStream): VerificationTypeInfo = {
        val tag = in.readUnsignedByte
        verification_type_info_reader(tag)(cp, in)
    }

    private val verification_type_info_reader = {

        import VerificationTypeInfoItem._

        val r = new Array[(Constant_Pool, DataInputStream) => VerificationTypeInfo](9)

        r(ITEM_Top.id) = (cp: Constant_Pool, in: DataInputStream) => TopVariableInfo()

        r(ITEM_Integer.id) = (cp: Constant_Pool, in: DataInputStream) => IntegerVariableInfo()

        r(ITEM_Float.id) = (cp: Constant_Pool, in: DataInputStream) => FloatVariableInfo()

        r(ITEM_Long.id) = (cp: Constant_Pool, in: DataInputStream) => LongVariableInfo()

        r(ITEM_Double.id) = (cp: Constant_Pool, in: DataInputStream) => DoubleVariableInfo()

        r(ITEM_Null.id) = (cp: Constant_Pool, in: DataInputStream) => NullVariableInfo()

        r(ITEM_UninitializedThis.id) =
            (cp: Constant_Pool, in: DataInputStream) => UninitializedThisVariableInfo()

        r(ITEM_Object.id) =
            (cp: Constant_Pool, in: DataInputStream) => ObjectVariableInfo(cp, in.readUnsignedShort)

        r(ITEM_Unitialized.id) =
            (cp: Constant_Pool, in: DataInputStream) =>
                UninitializedVariableInfo(in.readUnsignedShort)

        r
    }
}

object VerificationTypeInfoItem extends Enumeration {
    final val ITEM_Top = Value(0)
    final val ITEM_Integer = Value(1)
    final val ITEM_Float = Value(2)
    final val ITEM_Long = Value(4)
    final val ITEM_Double = Value(3)
    final val ITEM_Null = Value(5)
    final val ITEM_UninitializedThis = Value(6)
    final val ITEM_Object = Value(7)
    final val ITEM_Unitialized = Value(8)
}
