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
package resolved
package reader

import bat.reader.BootstrapMethods_attributeReader

import reflect.ClassTag

/**
 *
 * @author Michael Eichberg
 */
trait BootstrapMethods_attributeBinding
        extends BootstrapMethods_attributeReader
        with ConstantPoolBinding
        with AttributeBinding {

    type BootstrapMethods_attribute = resolved.BootstrapMethodTable

    type BootstrapMethod = resolved.BootstrapMethod
    val BootstrapMethodManifest: ClassTag[BootstrapMethod] = implicitly

    type BootstrapArgument = resolved.BootstrapArgument
    val BootstrapArgumentManifest: ClassTag[BootstrapArgument] = implicitly

    def BootstrapMethods_attribute(
        attributeNameIndex: Int,
        attributeLength: Int,
        bootstrapMethods: BootstrapMethods)(
            implicit cp: Constant_Pool): BootstrapMethods_attribute = {
        new BootstrapMethodTable(bootstrapMethods)
    }

    def BootstrapMethod(
        bootstrapMethodRef: Int,
        bootstrapArguments: BootstrapArguments)(
            implicit cp: Constant_Pool): BootstrapMethod = {
        new BootstrapMethod(cp(bootstrapMethodRef).asMethodHandle, bootstrapArguments)
    }

    def BootstrapArgument(
        constantPoolIndex: Int)(
            implicit cp: Constant_Pool): BootstrapArgument = {
        cp(constantPoolIndex).asBootstrapArgument
    }

}


