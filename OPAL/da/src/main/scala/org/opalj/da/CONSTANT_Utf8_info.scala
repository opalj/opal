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

import java.io.DataOutputStream
import java.io.ByteArrayOutputStream

import scala.xml.Node
import scala.xml.NodeSeq

/**
 * @author Michael Eichberg
 */
case class CONSTANT_Utf8_info(raw: Array[Byte], value: String) extends Constant_Pool_Entry {

    final override def size: Int = {
        // The length of the string in bytes is not equivalent to `value.length` due to the
        // usage of the modified UTF8 enconding.
        1 /* tag */ + 2 /* the length */ + raw.length /* the bytes of the string */
    }

    override def Constant_Type_Value = bi.ConstantPoolTags.CONSTANT_Utf8

    override def asConstantUTF8: this.type = this

    override def asString = value

    override def asCPNode(implicit cp: Constant_Pool): Node = {
        <span class="cp_entry">CONSTANT_Utf8_info("<span class="constant_value">{ value }</span>")</span>
    }

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        throw new UnsupportedOperationException
    }

    override def toString(implicit cp: Constant_Pool): String = value
}

object CONSTANT_Utf8 {

    def apply(value: String): CONSTANT_Utf8_info = {
        new CONSTANT_Utf8_info(
            {
                val bout = new ByteArrayOutputStream(value.length + 2)
                val dout = new DataOutputStream(bout)
                dout.writeUTF(value)
                dout.flush()
                bout.toByteArray()
            },
            value
        )
    }
}
