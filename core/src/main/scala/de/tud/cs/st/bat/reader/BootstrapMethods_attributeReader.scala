/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package reader

import java.io.DataInputStream

import scala.reflect.ClassTag

import de.tud.cs.st.util.ControlAbstractions.repeat

/**
 * Template method to read the (Java 7) BootstrapMethods attribute.
 *
 * '''From the Specification'''
 * The BootstrapMethods attribute is a variable-length attribute in the
 * attributes table of a ClassFile structure. The BootstrapMethods attribute
 * records bootstrap method specifiers referenced by invokedynamic instructions.
 *
 * {{{
 * BootstrapMethods_attribute {
 *  u2 attribute_name_index;
 *  u4 attribute_length;
 *  u2 num_bootstrap_methods;
 *  {    u2 bootstrap_method_ref;
 *      u2 num_bootstrap_arguments;
 *      u2 bootstrap_arguments[num_bootstrap_arguments]; 
 *  } bootstrap_methods[num_bootstrap_methods];
 * }
 * }}}
 * 
 * @author Michael Eichberg
 */
trait BootstrapMethods_attributeReader extends AttributeReader {

    //
    // ABSTRACT DEFINITIONS
    //

    type BootstrapMethods_attribute <: Attribute

    type BootstrapMethod
    implicit val BootstrapMethodManifest: ClassTag[BootstrapMethod]

    type BootstrapArgument
    implicit val BootstrapArgumentManifest: ClassTag[BootstrapArgument]

    def BootstrapMethods_attribute(attribute_name_index: Int,
                                   attribute_length: Int,
                                   bootstrap_methods: BootstrapMethods)(
                                       implicit constant_pool: Constant_Pool): BootstrapMethods_attribute

    def BootstrapMethod(bootstrap_method_ref: Int,
                        bootstrap_arguments: BootstrapArguments)(
                            implicit constant_pool: Constant_Pool): BootstrapMethod

    def BootstrapArgument(constant_pool_ref: Int)(implicit constant_pool: Constant_Pool): BootstrapArgument

    //
    // IMPLEMENTATION
    //

    type BootstrapMethods = IndexedSeq[BootstrapMethod]

    type BootstrapArguments = IndexedSeq[BootstrapArgument]

    def BootstrapArgument(in: DataInputStream, cp: Constant_Pool): BootstrapArgument = {
        BootstrapArgument(in.readUnsignedShort)(cp)
    }

    def BootstrapMethod(in: DataInputStream, cp: Constant_Pool): BootstrapMethod = {
        BootstrapMethod(
            in.readUnsignedShort,
            repeat(in.readUnsignedShort) {
                BootstrapArgument(in, cp)
            }
        )(cp)
    }

    register(
        BootstrapMethods_attributeReader.ATTRIBUTE_NAME ->
            ((ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                BootstrapMethods_attribute(
                    attribute_name_index, in.readInt, // attribute_length
                    repeat(in.readUnsignedShort) {
                        BootstrapMethod(in, cp)
                    }
                )(cp)
            })
    )
}

object BootstrapMethods_attributeReader {
    val ATTRIBUTE_NAME = "BootstrapMethods"
}




