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

    override def asInstructionParameter(implicit cp: Constant_Pool): NodeSeq = {
        <span class="ref">
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
