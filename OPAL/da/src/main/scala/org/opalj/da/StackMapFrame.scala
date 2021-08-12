/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node
import scala.xml.Text
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
sealed abstract class StackMapFrame {

    /**
     * The number of bytes required by the StackMapFrame in a class file.
     */
    def attribute_length: Int

    def frame_type: Int

    def toXHTML(
        implicit
        cp:                    Constant_Pool,
        previous_frame_offset: Int
    ): (Node, Int /* new offset*/ )

    protected def verification_type_infos_toXHTML(
        verification_type_infos: VerificationTypeInfos
    )(
        implicit
        cp: Constant_Pool
    ): NodeSeq = {
        NodeSeq.fromSeq(
            if (verification_type_infos.isEmpty) {
                List(<i>&lt;Empty&gt;</i>)
            } else {
                val vtis = verification_type_infos.map(l => l.toXHTML)
                val vtisIt = vtis.iterator
                val head = vtisIt.next()
                vtisIt.foldLeft(List(head)) { (r, n) => r ++ List(Text(", "), n) }
            }
        )
    }

}

case class SameFrame(frame_type: Int) extends StackMapFrame {

    final override def attribute_length: Int = 1

    override def toXHTML(
        implicit
        cp:                    Constant_Pool,
        previous_frame_offset: Int
    ): (Node, Int /* new offset*/ ) = {
        val newOffset = previous_frame_offset + frame_type + 1
        (
            <tr>
                <td>{ newOffset }</td>
                <td>SameFrame</td>
                <td>{ frame_type }</td>
                <td>{ frame_type }</td>
                <td>Stack:&nbsp;&lt;Empty&gt;<br/>Locals:&nbsp;Unchanged</td>
            </tr>,
            newOffset
        )
    }

}

case class SameLocals1StackItemFrame(
        frame_type:                   Int,
        verification_type_info_stack: VerificationTypeInfo
) extends StackMapFrame {

    final override def attribute_length: Int = 1 + verification_type_info_stack.attribute_length

    override def toXHTML(
        implicit
        cp:                    Constant_Pool,
        previous_frame_offset: Int
    ): (Node, Int /* new offset*/ ) = {
        val newOffset = previous_frame_offset + frame_type - 64 + 1
        (
            <tr>
                <td>{ newOffset }</td>
                <td>SameLocals1StackItemFrame</td>
                <td>{ frame_type }</td>
                <td>{ frame_type - 64 }</td>
                <td>
                    Stack:&nbsp;<i>&lt;Empty&gt;</i>
                    ,&nbsp;{ verification_type_info_stack.toXHTML(cp) }<br/>
                    Locals:&nbsp;Unchanged
                </td>
            </tr>,
            newOffset
        )
    }
}

case class SameLocals1StackItemFrameExtended(
        frame_type:                   Int                  = 247,
        offset_delta:                 Int,
        verification_type_info_stack: VerificationTypeInfo
) extends StackMapFrame {

    final override def attribute_length: Int = 1 + 2 + verification_type_info_stack.attribute_length

    override def toXHTML(
        implicit
        cp:                    Constant_Pool,
        previous_frame_offset: Int
    ): (Node, Int /* new offset*/ ) = {
        val newOffset = previous_frame_offset + offset_delta + 1
        (
            <tr>
                <td>{ newOffset }</td>
                <td>SameLocals1StackItemFrameExtended</td>
                <td>247</td>
                <td>{ offset_delta }</td>
                <td>
                    Stack:&nbsp;<i>&lt;Empty&gt;</i>
                    ,&nbsp;{ verification_type_info_stack.toXHTML(cp) }<br/>
                    Locals:&nbsp;Unchanged
                </td>
            </tr>,
            newOffset
        )
    }
}

case class ChopFrame(frame_type: Int, offset_delta: Int) extends StackMapFrame {

    final override def attribute_length: Int = 1 + 2

    override def toXHTML(
        implicit
        cp:                    Constant_Pool,
        previous_frame_offset: Int
    ): (Node, Int /* new offset*/ ) = {
        val newOffset = previous_frame_offset + offset_delta + 1
        (
            <tr>
                <td>{ newOffset }</td>
                <td>ChopFrame</td>
                <td>{ frame_type }</td>
                <td>{ offset_delta }</td>
                <td>
                    Stack:&nbsp;<i>&lt;Empty&gt;</i><br/>
                    Locals:&nbsp;The last&nbsp;{ 251 - frame_type }
                    locals(s) are absent
                </td>
            </tr>,
            newOffset
        )
    }
}

case class SameFrameExtended(frame_type: Int = 251, offset_delta: Int) extends StackMapFrame {

    final override def attribute_length: Int = 1 + 2

    override def toXHTML(
        implicit
        cp:                    Constant_Pool,
        previous_frame_offset: Int
    ): (Node, Int /* new offset*/ ) = {
        val newOffset = previous_frame_offset + offset_delta + 1
        (
            <tr>
                <td>{ newOffset }</td>
                <td>SameFrameExtended</td>
                <td>251</td>
                <td>{ offset_delta }</td>
                <td>
                    Stack:&nbsp;<i>&lt;Empty&gt;</i><br/>
                    Locals:&nbsp;Unchanged
                </td>
            </tr>,
            newOffset
        )
    }
}

case class AppendFrame(
        frame_type:                    Int,
        offset_delta:                  Int,
        verification_type_info_locals: VerificationTypeInfos
) extends StackMapFrame {

    final override def attribute_length: Int = {
        val initial = 1 + 2
        verification_type_info_locals.foldLeft(initial)((c, n) => c + n.attribute_length)
    }

    override def toXHTML(
        implicit
        cp:                    Constant_Pool,
        previous_frame_offset: Int
    ): (Node, Int /* new offset*/ ) = {
        val newOffset = previous_frame_offset + offset_delta + 1
        (
            <tr>
                <td>{ newOffset }</td>
                <td>AppendFrame</td>
                <td>{ frame_type }</td>
                <td>{ offset_delta }</td>
                <td>
                    Stack:&nbsp;<i>&lt;Empty&gt;</i><br/>
                    Locals:&nbsp;{ verification_type_infos_toXHTML(verification_type_info_locals) }
                </td>
            </tr>,
            newOffset
        )
    }

}

case class FullFrame(
        frame_type:                    Int,
        offset_delta:                  Int,
        verification_type_info_locals: VerificationTypeInfos,
        verification_type_info_stack:  VerificationTypeInfos
) extends StackMapFrame {

    final override def attribute_length: Int = {
        val initial = 1 + 2
        val locals = verification_type_info_locals.foldLeft(2 /*count*/ )(_ + _.attribute_length)
        val stack = verification_type_info_stack.foldLeft(2 /*count*/ )(_ + _.attribute_length)
        initial + locals + stack
    }

    override def toXHTML(
        implicit
        cp:                    Constant_Pool,
        previous_frame_offset: Int
    ): (Node, Int /* new offset*/ ) = {
        val newOffset = previous_frame_offset + offset_delta + 1
        (
            <tr>
                <td>{ newOffset }</td>
                <td>FullFrame</td>
                <td>{ frame_type }</td>
                <td>{ offset_delta }</td>
                <td>
                    Stack:&nbsp;{ verification_type_infos_toXHTML(verification_type_info_stack) }<br/>
                    Locals:&nbsp;{ verification_type_infos_toXHTML(verification_type_info_locals) }
                </td>
            </tr>,
            newOffset
        )
    }

}
