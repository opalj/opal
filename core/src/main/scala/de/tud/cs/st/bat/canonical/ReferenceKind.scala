/* 
   License (BSD Style License):
   Copyright (c) 2009, 2011
   Software Technology Group
   Department of Computer Science
   Technische Universität Darmstadt
   All rights reserved.
 
   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:
 
   - Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.
   - Redistributions in binary form must reproduce the above copyright notice,
     this list of conditions and the following disclaimer in the documentation
     and/or other materials provided with the distribution.
   - Neither the name of the Software Technology Group or Technische 
     Universität Darmstadt nor the names of its contributors may be used to 
     endorse or promote products derived from this software without specific 
     prior written permission.
 
   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
   ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
   LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
   INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
   CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
   ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
   POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.canonical

/**
 * Represents the different "reference_kind" values as used by the
 * CONSTANT_MethodHandle_info structure.
 *
 * @author Michael Eichberg
 */
object ReferenceKind extends Enumeration {

    type ReferenceKind = Value // this enables us to use the type "ReferenceKind" in our code!

    val REF_getField = Value(1, "REF_getField")

    val REF_getStatic = Value(2, "REF_getStatic")

    val REF_putField = Value(3, "REF_putField")

    val REF_putStatic = Value(4, "REF_putStatic")

    val REF_invokeVirtual = Value(5, "REF_invokeVirtual")

    val REF_invokeStatic = Value(6, "REF_invokeStatic")

    val REF_invokeSpecial = Value(7, "REF_invokeSpecial")

    val REF_newInvokeSpecial = Value(8, "REF_newInvokeSpecial")

    val REF_invokeInterface = Value(9, "REF_invokeInterface")

}
