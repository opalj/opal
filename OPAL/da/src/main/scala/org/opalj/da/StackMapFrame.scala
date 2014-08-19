/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
trait StackMapFrame {

    val frame_type: Int

    var initial_offset: Int = 0;

    def toXHTML(implicit cp: Constant_Pool, previous_fram_Offset: Int): Node
}

case class SameFrame(
        frame_type: Int) extends StackMapFrame {

    override def toXHTML(implicit cp: Constant_Pool, previous_fram_Offset: Int): Node = {
        <div>[pc: { initial_offset = previous_fram_Offset + frame_type + 1; initial_offset },frame_type:same]</div>
    }
}

case class SameLocals1StackItemFrame(
        frame_type: Int,
        verification_type_info_stack: VerificationTypeInfo) extends StackMapFrame {

    override def toXHTML(implicit cp: Constant_Pool, previous_fram_Offset: Int): Node = {
        <div>[pc: { initial_offset = previous_fram_Offset + frame_type - 64 + 1; initial_offset },frame_type:SameLocals1StackItem,stack:[{ verification_type_info_stack.toXHTML(cp) }]]</div>
    }
}

case class SameLocals1StackItemFrameExtended(
        frame_type: Int,
        offset_delta: Int,
        verification_type_info_stack: VerificationTypeInfo) extends StackMapFrame {

    override def toXHTML(implicit cp: Constant_Pool, previous_fram_Offset: Int): Node = {
        <div>
            [pc:{ initial_offset = previous_fram_Offset + offset_delta + 1; initial_offset }
            ,SameLocals1StackItemFrameExtended,stack:[{ verification_type_info_stack.toXHTML(cp) }
            ]]
        </div>
    }
}

case class ChopFrame(
        frame_type: Int,
        offset_delta: Int) extends StackMapFrame {

    override def toXHTML(implicit cp: Constant_Pool, previous_fram_Offset: Int): Node = {
        <div>[pc: { initial_offset = previous_fram_Offset + offset_delta + 1; initial_offset },frame_type:Chop]</div>
    }
}

case class SameFrameExtended(
        frame_type: Int,
        offset_delta: Int) extends StackMapFrame {

    override def toXHTML(implicit cp: Constant_Pool, previous_fram_Offset: Int): Node = {
        <div>
            [pc:{ initial_offset = previous_fram_Offset + offset_delta + 1; initial_offset }
            ,frame_type:SameFrameExtended]
        </div>
    }
}

case class AppendFrame(
        frame_type: Int,
        offset_delta: Int,
        verification_type_info_locals: Seq[VerificationTypeInfo]) extends StackMapFrame {

    private def localsToXHTML(implicit cp: Constant_Pool): Node = {
        <span> { for (verification_type_info_local ← verification_type_info_locals) yield verification_type_info_local.toXHTML(cp) }</span>
    }

    override def toXHTML(implicit cp: Constant_Pool, previous_fram_Offset: Int): Node = {
        <div>
            [pc:{ initial_offset = previous_fram_Offset + offset_delta + 1; initial_offset }
            ,Append,locals:[{ localsToXHTML(cp) }
            ]]
        </div>
    }

}

case class FullFrame(
        frame_type: Int,
        offset_delta: Int,
        verification_type_info_locals: IndexedSeq[VerificationTypeInfo],
        verification_type_info_stack: IndexedSeq[VerificationTypeInfo]) extends StackMapFrame {

    private def localsToXHTML(implicit cp: Constant_Pool): Node = {
        <span> { for (verification_type_info_local ← verification_type_info_locals) yield verification_type_info_local.toXHTML(cp) }</span>
    }

    private def stackToXHTML(implicit cp: Constant_Pool): Node = {
        <span> { for (verification_type_info_stack_ ← verification_type_info_stack) yield verification_type_info_stack_.toXHTML(cp) }</span>
    }

    override def toXHTML(implicit cp: Constant_Pool, previous_fram_Offset: Int): Node = {
        <div>
            [pc:{ initial_offset = previous_fram_Offset + offset_delta + 1; initial_offset }
            ,Full,locals:[{ localsToXHTML(cp) }
            ],stack:[{ stackToXHTML(cp) }
            ]]
        </div>
    }

}