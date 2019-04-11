/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 */
trait CONSTANT_Ref extends Constant_Pool_Entry {

    final override def size: Int = 1 + 2 + 2

    val class_index: Constant_Pool_Index

    val name_and_type_index: Constant_Pool_Index

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <div class="cp_entry">
            { this.getClass().getSimpleName }
            (<div class="cp_ref">
                 class_index={ class_index }
                 &laquo;
                 { cp(class_index).asCPNode }
                 &raquo;
             </div>
            <div class="cp_ref">
                name_and_type_index={ name_and_type_index }
                &laquo;
                { cp(name_and_type_index).asCPNode }
                &raquo;
            </div>
            )
        </div>

    def asInstructionParameter(classType: Option[String])(implicit cp: Constant_Pool): NodeSeq = {
        <span class="ref">
            { if (classType.isDefined) <span>{ classType.get }&nbsp;</span> else NodeSeq.Empty }
            { asJavaReferenceType(class_index).asSpan("") }
            <span>{{ { cp(name_and_type_index).asInstructionParameter } }}</span>
        </span>
    }

    override def toString(implicit cp: Constant_Pool): String = {
        cp(class_index).toString(cp)+"{ "+cp(name_and_type_index).toString(cp)+" }"
    }

}

object CONSTANT_Ref {

    def unapply(ref: CONSTANT_Ref): Option[(Constant_Pool_Index, Constant_Pool_Index)] = {
        Some((ref.class_index, ref.name_and_type_index))
    }

}
