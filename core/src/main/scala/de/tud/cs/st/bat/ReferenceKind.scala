/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat

/**
 * Represents the different "reference_kind" values as used by the constant pool's
 * CONSTANT_MethodHandle_info structure.
 *
 * @author Michael Eichberg
 */
sealed abstract class ReferenceKind {

    def referenceKind: Int

    def referenceKindName: String

}
object ReferenceKind {

    private[this] val referenceKinds: Array[ReferenceKind] =
        Array(
            null, // <=> Index 0
            REF_getField,
            REF_getStatic,
            REF_putField,
            REF_putStatic,
            REF_invokeVirtual,
            REF_invokeStatic,
            REF_invokeSpecial,
            REF_newInvokeSpecial,
            REF_invokeInterface
        )

    def apply(referenceKindID: Int) = referenceKinds(referenceKindID)
}

case object REF_getField extends ReferenceKind {
    final val referenceKind = 1
    final val referenceKindName = "REF_getField"
}

case object REF_getStatic extends ReferenceKind {
    final val referenceKind = 2
    final val referenceKindName = "REF_getStatic"
}

case object REF_putField extends ReferenceKind {
    final val referenceKind = 3
    final val referenceKindName = "REF_putField"
}

case object REF_putStatic extends ReferenceKind {
    final val referenceKind = 4
    final val referenceKindName = "REF_putStatic"
}

case object REF_invokeVirtual extends ReferenceKind {
    final val referenceKind = 5
    final val referenceKindName = "REF_invokeVirtual"
}

case object REF_invokeStatic extends ReferenceKind {
    final val referenceKind = 6
    final val referenceKindName = "REF_invokeStatic"
}

case object REF_invokeSpecial extends ReferenceKind {
    final val referenceKind = 7
    final val referenceKindName = "REF_invokeSpecial"
}

case object REF_newInvokeSpecial extends ReferenceKind {
    final val referenceKind = 8
    final val referenceKindName = "REF_newInvokeSpecial"
}

case object REF_invokeInterface extends ReferenceKind {
    final val referenceKind = 9
    final val referenceKindName = "REF_invokeInterface"
}
  

