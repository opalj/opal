/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi

/**
 * A constant pool tag identifies the type of a specific entry in the constant pool.
 *
 * @author Michael Eichberg
 */
object ConstantPoolTags extends Enumeration {

    // IN THE FOLLOWING THE ORDER IS AS DEFINED IN THE JVM SPECIFICATION

    final val CONSTANT_Class_ID = 7

    final val CONSTANT_Fieldref_ID = 9
    final val CONSTANT_Methodref_ID = 10
    final val CONSTANT_InterfaceMethodref_ID = 11

    final val CONSTANT_String_ID = 8
    final val CONSTANT_Integer_ID = 3
    final val CONSTANT_Float_ID = 4
    final val CONSTANT_Long_ID = 5
    final val CONSTANT_Double_ID = 6

    final val CONSTANT_NameAndType_ID = 12

    final val CONSTANT_Utf8_ID = 1

    // Java 7 onwards...
    final val CONSTANT_MethodHandle_ID = 15
    final val CONSTANT_MethodType_ID = 16
    final val CONSTANT_InvokeDynamic_ID = 18

    // Java 9 onwards...
    final val CONSTANT_Module_ID = 19
    final val CONSTANT_Package_ID = 20

    // Java 11 onwards...
    final val CONSTANT_Dynamic_ID = 17

    // THE ENUM VALUES

    final val CONSTANT_Class = Value(CONSTANT_Class_ID, "CONSTANT_Class")

    final val CONSTANT_Fieldref = Value(CONSTANT_Fieldref_ID, "CONSTANT_Fieldref")
    final val CONSTANT_Methodref = Value(CONSTANT_Methodref_ID, "CONSTANT_Methodref")
    final val CONSTANT_InterfaceMethodref = {
        Value(CONSTANT_InterfaceMethodref_ID, "CONSTANT_InterfaceMethodref")
    }

    final val CONSTANT_String = Value(CONSTANT_String_ID, "CONSTANT_String")
    final val CONSTANT_Integer = Value(CONSTANT_Integer_ID, "CONSTANT_Integer")
    final val CONSTANT_Float = Value(CONSTANT_Float_ID, "CONSTANT_Float")
    final val CONSTANT_Long = Value(CONSTANT_Long_ID, "CONSTANT_Long")
    final val CONSTANT_Double = Value(CONSTANT_Double_ID, "CONSTANT_Double")

    final val CONSTANT_NameAndType = Value(CONSTANT_NameAndType_ID, "CONSTANT_NameAndType")

    final val CONSTANT_Utf8 = Value(CONSTANT_Utf8_ID, "CONSTANT_Utf8")

    final val CONSTANT_MethodHandle = Value(CONSTANT_MethodHandle_ID, "CONSTANT_MethodHandle")
    final val CONSTANT_MethodType = Value(CONSTANT_MethodType_ID, "CONSTANT_MethodType")
    final val CONSTANT_InvokeDynamic = Value(CONSTANT_InvokeDynamic_ID, "CONSTANT_InvokeDynamic")

    final val CONSTANT_Module = Value(CONSTANT_Module_ID, "CONSTANT_Module")
    final val CONSTANT_Package = Value(CONSTANT_Package_ID, "CONSTANT_Package")

    final val CONSTANT_Dynamic = Value(CONSTANT_Dynamic_ID, "CONSTANT_Dynamic")
}
