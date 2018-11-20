/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 * @author Tobias Becker
 */
trait Constant_Pool_Entry extends bi.reader.ConstantPoolEntry {

    /**
     * Size of this constant pool entry in bytes.
     */
    def size: Int

    def Constant_Type_Value: bi.ConstantPoolTags.Value

    final def tag: Int = Constant_Type_Value.id

    def asConstantClass: CONSTANT_Class_info = throw new ClassCastException();
    def asConstantUTF8: CONSTANT_Utf8_info = throw new ClassCastException();
    final def asConstantModule: CONSTANT_Module_info = this.asInstanceOf[CONSTANT_Module_info]
    final def asConstantPackage: CONSTANT_Package_info = this.asInstanceOf[CONSTANT_Package_info]

    /**
     * Creates a one-to-one representation of this constant pool entry node. The
     * created representation is intended to be used to completely represent this
     * constant pool entry.
     */
    def asCPNode(implicit cp: Constant_Pool): Node

    //// OLD CONVERSION Methods

    def asString: String = throw new UnsupportedOperationException()

    def toString(implicit cp: Constant_Pool): String

    /**
     * Creates a resolved representation of this constant pool entry that is well-suited as an
     * output in combination with an instruction (e.g., an `ldc`, `get|putfield`,
     * `invokXYZ`,...). I.e., a representation that contains no more pointers in the CP.
     */
    def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq

}
