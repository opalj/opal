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

    def asConstantClass: CONSTANT_Class_info = throw new ClassCastException()
    def asConstantUTF8: CONSTANT_Utf8_info = throw new ClassCastException()
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
