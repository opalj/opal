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
import org.opalj.bi.ConstantPoolTag

import scala.xml.NodeSeq

/**
 *
 * @author Michael Eichberg
 */
case class CONSTANT_Integer_info(value: Int) extends Constant_Pool_Entry {

    final override def size: Int = 1 + 4

    override def Constant_Type_Value: ConstantPoolTag = bi.ConstantPoolTags.CONSTANT_Integer

    override def asCPNode(implicit cp: Constant_Pool): Node =
        <span class="cp_entry">
            CONSTANT_Integer_info(
            <span class="constant_value">{ value }</span>
            )
        </span>

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        val repr =
            if (value < 0 || value >= 10) {
                var additionalInfo = " = 0x"+value.toHexString
                if (value == Int.MinValue)
                    additionalInfo += " = Int.Min"
                else if (value == Int.MaxValue)
                    additionalInfo += " = Int.Max"
                Seq(
                    Text(value.toString),
                    <span class="comment">{ additionalInfo }</span>
                )
            } else {
                Seq(Text(value.toString))
            }

        <span class="constant_value">{ repr }</span>
    }

    override def toString(implicit cp: Constant_Pool): String = {
        if (value < 0 || value >= 10) {
            var r = value+" (= 0x"+value.toHexString
            if (value == Int.MinValue)
                r += " = Int.Min"
            else if (value == Int.MaxValue)
                r += " = Int.Max"
            r+")"
        } else {
            value.toString
        }
    }
}
