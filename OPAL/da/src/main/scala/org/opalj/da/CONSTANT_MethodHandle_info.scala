/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

import org.opalj.bi.ConstantPoolTag

/**
 *
 * @author Michael Eichberg
 */
case class CONSTANT_MethodHandle_info(
        reference_kind:  Int,
        reference_index: Constant_Pool_Index
) extends Constant_Pool_Entry {

    final override def size: Int = 1 + 1 + 2

    override def Constant_Type_Value: ConstantPoolTag = bi.ConstantPoolTags.CONSTANT_MethodHandle

    def refrenceKindAsNode(implicit cp: Constant_Pool): Node = {
        <span class="method_handle_reference_kind">{
            reference_kind match {
                case 1 => "REF_getField getfield C.f: T"
                case 2 => "REF_getStatic getstatic C.f:T"
                case 3 => "REF_putField putfield C.f:T"
                case 4 => "REF_putStatic putstatic C.f:T"
                case 5 => "REF_invokeVirtual invokevirtual C.m:(A*)T"
                case 6 =>
                    cp(reference_index) match {
                        case _: CONSTANT_InterfaceMethodref_info =>
                            "REF_invokeStatic(interface) invokestatic C.m:(A*)T"
                        case _: CONSTANT_Methodref_info =>
                            "REF_invokeStatic(class) invokestatic C.m:(A*)T"
                    }
                case 7 =>
                    cp(reference_index) match {
                        case _: CONSTANT_InterfaceMethodref_info =>
                            "REF_invokeSpecial(interface) invokeSpecial C.m:(A*)T"
                        case _: CONSTANT_Methodref_info =>
                            "REF_invokeSpecial(class) invokeSpecial C.m:(A*)T"
                    }
                case 8 => "REF_newInvokeSpecial new C; dup; invokespecial C.<init>:(A*)V"
                case 9 => "REF_invokeInterface invokeinterface C.m:(A*)T"
            }
        }</span>
    }

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <span class="cp_entry">
            { this.getClass.getSimpleName }
            (reference_kind={ reference_kind }
            /*
            { refrenceKindAsNode }
            */,
            reference_index={ reference_index }
            /*
            <span class="cp_ref">{ cp(reference_index).asCPNode }</span>
            */)
        </span>

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq =
        <span class="method_handle">MethodHandle(kind={
            refrenceKindAsNode
        }, {
            cp(reference_index).asInstructionParameter
        })</span>

    override def toString(implicit cp: Constant_Pool): String = {
        val reference_indexAsString = cp(reference_index).toString(cp)
        s"CONSTANT_MethodHandle_info($reference_kind, $reference_indexAsString/*$reference_index */)"
    }
}
