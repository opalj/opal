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

case class CONSTANT_NameAndType_info(
        name_index: Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index) extends Constant_Pool_Entry {

    override def Constant_Type_Value = bi.ConstantPoolTags.CONSTANT_NameAndType

    def toString(implicit cp: Constant_Pool): String = {
        // TODO Validate the implementation - looks confusing
        if (!(cp(descriptor_index).toString(cp).charAt(0) == '('))
            cp(name_index).toString(cp)+" : "+parseFieldType(cp(descriptor_index).asString)
        else if ("<init>" == cp(name_index).asString)
            cp(name_index).toString(cp)
        else
            "."+cp(name_index).toString(cp)+" : "+
                parseMethodDescriptor(cp(name_index).asString, cp(descriptor_index).asString)
    }

    def toLDCString(implicit cp: Constant_Pool): String =
        throw new UnsupportedOperationException
}

