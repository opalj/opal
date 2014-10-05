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
 *
 * @author Michael Eichberg
 */
case class CONSTANT_MethodHandle_info(
        reference_kind: Int,
        reference_index: Constant_Pool_Index) extends Constant_Pool_Entry {

    override def Constant_Type_Value = bi.ConstantPoolTags.CONSTANT_MethodHandle

    def toNode(implicit cp: Constant_Pool): Node =
        <span class="cp_entry">
            { this.getClass().getSimpleName }
            (reference_kind={ reference_kind }
            /*
            <span class="method_handle_reference_kind">{
                reference_kind match {
                    case 1⇒ "REF_getField getfield C.f: T"
                    case 2⇒ "REF_getStatic getstatic C.f:T"
                    case 3⇒ "REF_putField putfield C.f:T"
                    case 4⇒ "REF_putStatic putstatic C.f:T"
                    case 5⇒ "REF_invokeVirtual invokevirtual C.m:(A*)T"
                    case 6⇒ "REF_invokeStatic invokestatic C.m:(A*)T"
                    case 7⇒ "REF_invokeSpecial invokespecial C.m:(A*)T"
                    case 8⇒ "REF_newInvokeSpecial new C; dup; invokespecial C.<init>:(A*)V"
                    case 9⇒ "REF_invokeInterface invokeinterface C.m:(A*)T"
                }
            }</span>
            */,
            reference_index={ reference_index }
            /*
            <span class="cp_ref">{ cp(reference_index).toNode(cp) }</span>
            */)
        </span>

    def toString(implicit cp: Constant_Pool): String = {
        s"CONSTANT_MethodHandle_info($reference_kind ,${cp(reference_index).toString(cp)}/*$reference_index */)"
    }

    def toLDCString(implicit cp: Constant_Pool): String =
        s"Kind $reference_kind: ${cp(reference_index).toString(cp)}"
}
