/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.reflect.ClassTag

import scala.collection.mutable

import org.opalj.bi.reader.Constant_PoolAbstractions
import org.opalj.bi.reader.Constant_PoolReader

/**
 * Representation of the constant pool as specified by the JVM Specification (Java 8).
 * (This representation does not provide any abstraction.)
 *
 * @author Michael Eichberg
 */
trait Constant_PoolBinding extends Constant_PoolReader with Constant_PoolAbstractions {

    // HERE, WE DON'T NEED A DEFERRED ACTIONS STORE
    protected[this] def createDeferredActionsStore(): DeferredActionsStore = {
        new mutable.ArrayBuffer[ClassFile => ClassFile] with Constant_Pool_Entry {
            override def Constant_Type_Value: Nothing = {
                throw new UnsupportedOperationException()
            }

            override def asCPNode(implicit cp: Constant_Pool): Nothing = {
                throw new UnsupportedOperationException()
            }

            override def asInstructionParameter(implicit cp: Constant_Pool): Nothing = {
                throw new UnsupportedOperationException()
            }

            override def toString(implicit cp: Constant_Pool): Nothing = {
                throw new UnsupportedOperationException(
                    this.getClass.toString+" does not support toString(cp)"
                )
            }
        }
    }

    // ______________________________________________________________________________________________
    //
    // REPRESENTATION OF THE CONSTANT POOL
    // ______________________________________________________________________________________________
    //

    type Constant_Pool_Entry = org.opalj.da.Constant_Pool_Entry
    override implicit val constantPoolEntryType: ClassTag[Constant_Pool_Entry] = ClassTag(classOf[org.opalj.da.Constant_Pool_Entry])

    type CONSTANT_Class_info = org.opalj.da.CONSTANT_Class_info
    def CONSTANT_Class_info(i: Int): CONSTANT_Class_info = da.CONSTANT_Class_info(i)

    type CONSTANT_Double_info = org.opalj.da.CONSTANT_Double_info
    def CONSTANT_Double_info(d: Double): CONSTANT_Double_info = new CONSTANT_Double_info(d)

    type CONSTANT_Float_info = org.opalj.da.CONSTANT_Float_info
    def CONSTANT_Float_info(f: Float): CONSTANT_Float_info = new CONSTANT_Float_info(f)

    type CONSTANT_Integer_info = org.opalj.da.CONSTANT_Integer_info
    def CONSTANT_Integer_info(i: Int): CONSTANT_Integer_info = new CONSTANT_Integer_info(i)

    type CONSTANT_Long_info = org.opalj.da.CONSTANT_Long_info
    def CONSTANT_Long_info(l: Long): CONSTANT_Long_info = new CONSTANT_Long_info(l)

    type CONSTANT_Utf8_info = org.opalj.da.CONSTANT_Utf8_info
    def CONSTANT_Utf8_info(r: Array[Byte], s: String): CONSTANT_Utf8_info = {
        new CONSTANT_Utf8_info(r, s)
    }

    type CONSTANT_String_info = org.opalj.da.CONSTANT_String_info
    def CONSTANT_String_info(i: Int): CONSTANT_String_info = new CONSTANT_String_info(i)

    type CONSTANT_Fieldref_info = org.opalj.da.CONSTANT_Fieldref_info
    def CONSTANT_Fieldref_info(
        class_index: Constant_Pool_Index, name_and_type_index: Constant_Pool_Index
    ): CONSTANT_Fieldref_info = {
        new CONSTANT_Fieldref_info(class_index, name_and_type_index)
    }

    type CONSTANT_Methodref_info = org.opalj.da.CONSTANT_Methodref_info
    def CONSTANT_Methodref_info(
        class_index: Constant_Pool_Index, name_and_type_index: Constant_Pool_Index
    ): CONSTANT_Methodref_info = {
        new CONSTANT_Methodref_info(class_index, name_and_type_index)
    }

    type CONSTANT_InterfaceMethodref_info = org.opalj.da.CONSTANT_InterfaceMethodref_info
    def CONSTANT_InterfaceMethodref_info(
        class_index: Constant_Pool_Index, name_and_type_index: Constant_Pool_Index
    ): CONSTANT_InterfaceMethodref_info = {
        new CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index)
    }

    type CONSTANT_NameAndType_info = org.opalj.da.CONSTANT_NameAndType_info
    def CONSTANT_NameAndType_info(
        name_index: Constant_Pool_Index, descriptor_index: Constant_Pool_Index
    ): CONSTANT_NameAndType_info = {
        new CONSTANT_NameAndType_info(name_index, descriptor_index)
    }

    type CONSTANT_InvokeDynamic_info = org.opalj.da.CONSTANT_InvokeDynamic_info
    def CONSTANT_InvokeDynamic_info(
        bootstrap_method_attr_index: Int,
        name_and_type_index:         Constant_Pool_Index
    ): org.opalj.da.ClassFileReader.CONSTANT_InvokeDynamic_info = {
        new CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index, name_and_type_index)
    }

    type CONSTANT_MethodHandle_info = org.opalj.da.CONSTANT_MethodHandle_info
    def CONSTANT_MethodHandle_info(
        reference_kind:  Int,
        reference_index: Constant_Pool_Index
    ): org.opalj.da.ClassFileReader.CONSTANT_MethodHandle_info = {
        new CONSTANT_MethodHandle_info(reference_kind, reference_index)
    }

    type CONSTANT_MethodType_info = org.opalj.da.CONSTANT_MethodType_info
    def CONSTANT_MethodType_info(
        descriptor_index: Constant_Pool_Index
    ): org.opalj.da.ClassFileReader.CONSTANT_MethodType_info = {
        new CONSTANT_MethodType_info(descriptor_index)
    }

    type CONSTANT_Module_info = org.opalj.da.CONSTANT_Module_info
    def CONSTANT_Module_info(i: Int): CONSTANT_Module_info = da.CONSTANT_Module_info(i)

    type CONSTANT_Package_info = org.opalj.da.CONSTANT_Package_info
    def CONSTANT_Package_info(i: Int): CONSTANT_Package_info = da.CONSTANT_Package_info(i)

    type CONSTANT_Dynamic_info = org.opalj.da.CONSTANT_Dynamic_info
    def CONSTANT_Dynamic_info(
        bootstrap_method_attr_index: Int,
        name_and_type_index:         Constant_Pool_Index
    ): org.opalj.da.ClassFileReader.CONSTANT_Dynamic_info = {
        new CONSTANT_Dynamic_info(bootstrap_method_attr_index, name_and_type_index)
    }
}
