/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import java.io.ByteArrayInputStream

import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * Defines a template method to read in a class file's constant pool.
 */
trait Constant_PoolReader extends Constant_PoolAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type CONSTANT_Class_info <: Constant_Pool_Entry
    type CONSTANT_Fieldref_info <: Constant_Pool_Entry
    type CONSTANT_Methodref_info <: Constant_Pool_Entry
    type CONSTANT_InterfaceMethodref_info <: Constant_Pool_Entry
    type CONSTANT_String_info <: Constant_Pool_Entry
    type CONSTANT_Integer_info <: Constant_Pool_Entry
    type CONSTANT_Float_info <: Constant_Pool_Entry
    type CONSTANT_Long_info <: Constant_Pool_Entry
    type CONSTANT_Double_info <: Constant_Pool_Entry
    type CONSTANT_NameAndType_info <: Constant_Pool_Entry
    type CONSTANT_Utf8_info <: Constant_Pool_Entry
    type CONSTANT_MethodHandle_info <: Constant_Pool_Entry
    type CONSTANT_MethodType_info <: Constant_Pool_Entry
    type CONSTANT_InvokeDynamic_info <: Constant_Pool_Entry
    type CONSTANT_Module_info <: Constant_Pool_Entry
    type CONSTANT_Package_info <: Constant_Pool_Entry
    type CONSTANT_Dynamic_info <: Constant_Pool_Entry

    //
    // FACTORY METHODS
    //

    protected def CONSTANT_Class_info(i: Int): CONSTANT_Class_info
    protected def CONSTANT_Fieldref_info(class_index: Int, name_and_type_index: Int): CONSTANT_Fieldref_info
    protected def CONSTANT_Methodref_info(class_index: Int, name_and_type_index: Int): CONSTANT_Methodref_info
    protected def CONSTANT_InterfaceMethodref_info(class_index: Int, name_and_type_index: Int): CONSTANT_InterfaceMethodref_info
    protected def CONSTANT_String_info(i: Int): CONSTANT_String_info
    protected def CONSTANT_Integer_info(i: Int): CONSTANT_Integer_info
    protected def CONSTANT_Float_info(f: Float): CONSTANT_Float_info
    protected def CONSTANT_Long_info(l: Long): CONSTANT_Long_info
    protected def CONSTANT_Double_info(d: Double): CONSTANT_Double_info
    protected def CONSTANT_NameAndType_info(name_index: Int, descriptor_index: Int): CONSTANT_NameAndType_info
    protected def CONSTANT_Utf8_info(r: Array[Byte], s: String): CONSTANT_Utf8_info
    // JAVA 7 Constant Pool Entries
    protected def CONSTANT_MethodHandle_info(reference_kind: Int, reference_index: Int): CONSTANT_MethodHandle_info
    protected def CONSTANT_MethodType_info(descriptor_index: Int): CONSTANT_MethodType_info
    protected def CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index: Int, name_and_type_index: Int): CONSTANT_InvokeDynamic_info
    // JAVA 9 Constant Pool Entries
    protected def CONSTANT_Module_info(name_index: Int): CONSTANT_Module_info
    protected def CONSTANT_Package_info(name_index: Int): CONSTANT_Package_info
    // JAVA 11 Constant Pool Entries
    protected def CONSTANT_Dynamic_info(bootstrap_method_attr_index: Int, name_and_type_index: Int): CONSTANT_Dynamic_info

    /**
     * Creates a storage area for functions that will be called after the class file was
     * completely loaded. This makes it possible to register functions that are newly
     * created for a special class file object to perform actions related to that specific
     * class file object. For further information study the resolving process of
     * `invokedynamic` instructions.
     */
    protected[this] def createDeferredActionsStore(): DeferredActionsStore

    //
    // IMPLEMENTATION
    //

    def registerDeferredAction(cp: Constant_Pool)(deferredAction: ClassFile => ClassFile): Unit = {
        val store = cp(0).asInstanceOf[DeferredActionsStore]
        store.synchronized { store += deferredAction }
    }

    override def applyDeferredActions(cp: Constant_Pool, classFile: ClassFile): ClassFile = {
        var transformedClassFile = classFile
        val das = cp(0).asInstanceOf[DeferredActionsStore]
        das.foreach { deferredAction => transformedClassFile = deferredAction(transformedClassFile) }
        das.clear()
        transformedClassFile
    }

    import ConstantPoolTags._

    def Constant_Pool(in: DataInputStream): Constant_Pool = {

        /*
         * The value of the constant_pool_count item is equal to the
         * number of entries in the constant_pool table plus one. A
         * constant_pool index is considered valid if it is greater than zero
         * and less than constant_pool_count.
         *
         * We use position zero in the constant pool table to store functions that need
         * to be performed after the entire class is loaded.
         *
         * E.g., the references of `invokedynamic` instructions can only be resolved after
         * the `BootstrapMethods` attribute was loaded.
         */
        val constant_pool_count = in.readUnsignedShort

        /*
         * The format of each constant_pool table entry is indicated by its ﬁrst
         * “tag” byte.
         *
         * The constant_pool table is indexed from 1 to constant_pool_count−1.
         */
        val constant_pool_entries = new Array[Constant_Pool_Entry](constant_pool_count)

        constant_pool_entries(0) = createDeferredActionsStore()

        var i = 1
        while (i < constant_pool_count) {
            val tag = in.readUnsignedByte
            constant_pool_entries(i) = (tag: @scala.annotation.switch) match {
                case CONSTANT_Class_ID =>
                    i += 1
                    CONSTANT_Class_info(in.readUnsignedShort)
                case CONSTANT_Fieldref_ID =>
                    i += 1
                    CONSTANT_Fieldref_info(in.readUnsignedShort, in.readUnsignedShort)
                case CONSTANT_Methodref_ID =>
                    i += 1
                    CONSTANT_Methodref_info(in.readUnsignedShort, in.readUnsignedShort)
                case CONSTANT_InterfaceMethodref_ID =>
                    i += 1
                    CONSTANT_InterfaceMethodref_info(in.readUnsignedShort, in.readUnsignedShort)
                case CONSTANT_String_ID =>
                    i += 1
                    CONSTANT_String_info(in.readUnsignedShort)
                case CONSTANT_Integer_ID =>
                    i += 1
                    CONSTANT_Integer_info(in.readInt)
                case CONSTANT_Float_ID =>
                    i += 1
                    CONSTANT_Float_info(in.readFloat)
                case CONSTANT_Long_ID =>
                    i += 2
                    CONSTANT_Long_info(in.readLong)
                case CONSTANT_Double_ID =>
                    i += 2
                    CONSTANT_Double_info(in.readDouble)
                case CONSTANT_NameAndType_ID =>
                    i += 1
                    CONSTANT_NameAndType_info(in.readUnsignedShort, in.readUnsignedShort)
                case CONSTANT_Utf8_ID =>
                    i += 1
                    if (in.markSupported()) {
                        in.mark(UShort.MaxValue + 2)
                        val length = in.readUnsignedShort
                        val raw = new Array[Byte](length)
                        in.readFully(raw)
                        in.reset()
                        val value = in.readUTF()
                        CONSTANT_Utf8_info(raw, value)
                    } else {
                        val size = in.readUnsignedShort
                        val raw = new Array[Byte](size)
                        in.readFully(raw)
                        val data = new Array[Byte](size + 2)
                        data(0) = (0xff & (size >> 8)).toByte
                        data(1) = (0xff & size).toByte
                        System.arraycopy(raw, 0, data, 2, size)
                        val tin = new DataInputStream(new ByteArrayInputStream(data))
                        CONSTANT_Utf8_info(raw, tin.readUTF)
                    }

                case CONSTANT_MethodHandle_ID =>
                    i += 1
                    CONSTANT_MethodHandle_info(in.readUnsignedByte, in.readUnsignedShort)
                case CONSTANT_MethodType_ID =>
                    i += 1
                    CONSTANT_MethodType_info(in.readUnsignedShort)
                case CONSTANT_InvokeDynamic_ID =>
                    i += 1
                    CONSTANT_InvokeDynamic_info(in.readUnsignedShort, in.readUnsignedShort)

                case CONSTANT_Module_ID =>
                    i += 1
                    CONSTANT_Module_info(in.readUnsignedShort)
                case CONSTANT_Package_ID =>
                    i += 1
                    CONSTANT_Package_info(in.readUnsignedShort)

                case CONSTANT_Dynamic_ID =>
                    i += 1
                    CONSTANT_Dynamic_info(in.readUnsignedShort, in.readUnsignedShort)

                case _ =>
                    val header = s"wrong constant pool tag: $tag (entry: $i/$constant_pool_count); "
                    val message =
                        constant_pool_entries.iterator.zipWithIndex.slice(1, i).map(_.swap).
                            mkString(header+"previous entries:\n\t", "\n\t", "\n")
                    throw new BytecodeProcessingFailedException(message)
            }
        }
        constant_pool_entries
    }
}
