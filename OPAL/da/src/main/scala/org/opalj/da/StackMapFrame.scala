/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
        verification_type_infos: Seq[VerificationTypeInfo]
    )(
        implicit
        cp: Constant_Pool
    ): NodeSeq = {
        NodeSeq.fromSeq(
            if (verification_type_infos.isEmpty) {
                List(<i>&lt;Empty&gt;</i>)
            } else {
                val vtis = verification_type_infos.map(l ⇒ { l.toXHTML })
                vtis.tail.foldLeft(List(vtis.head)) { (r, n) ⇒ r ++ List(Text(", "), n) }
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
        verification_type_info_locals: Seq[VerificationTypeInfo]
) extends StackMapFrame {

    final override def attribute_length: Int = {
        val initial = 1 + 2
        verification_type_info_locals.foldLeft(initial)((c, n) ⇒ c + n.attribute_length)
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
        verification_type_info_locals: IndexedSeq[VerificationTypeInfo],
        verification_type_info_stack:  IndexedSeq[VerificationTypeInfo]
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
