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
package bi
package reader

import java.io.DataInputStream

import scala.reflect.ClassTag

/**
 * Template method to read the (Java 7) ''BootstrapMethods'' attribute.
 *
 * '''From the Specification'''
 * The `BootstrapMethods` attribute is a variable-length attribute in the
 * attributes table of a `ClassFile` structure. The `BootstrapMethods` attribute
 * records bootstrap method specifiers referenced by `invokedynamic` instructions.
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

    def BootstrapMethods_attribute(
        constant_pool: Constant_Pool,
        attribute_name_index: Int,
        attribute_length: Int,
        bootstrap_methods: BootstrapMethods): BootstrapMethods_attribute

    def BootstrapMethod(
        constant_pool: Constant_Pool,
        bootstrap_method_ref: Int,
        bootstrap_arguments: BootstrapArguments): BootstrapMethod

    def BootstrapArgument(
        constant_pool: Constant_Pool,
        constant_pool_ref: Int): BootstrapArgument

    //
    // IMPLEMENTATION
    //

    type BootstrapMethods = IndexedSeq[BootstrapMethod]

    type BootstrapArguments = IndexedSeq[BootstrapArgument]

    def BootstrapArgument(cp: Constant_Pool, in: DataInputStream): BootstrapArgument = {
        BootstrapArgument(cp, in.readUnsignedShort)
    }

    def BootstrapMethod(cp: Constant_Pool, in: DataInputStream): BootstrapMethod = {
        BootstrapMethod(
            cp,
            in.readUnsignedShort,
            repeat(in.readUnsignedShort) {
                BootstrapArgument(cp, in)
            }
        )
    }

    /* Structure
     * <pre>
     * BootstrapMethods_attribute {
     *  u2 attribute_name_index;
     *  u4 attribute_length;
     *  u2 num_bootstrap_methods;
     *  {   u2 bootstrap_method_ref;
     *      u2 num_bootstrap_arguments;
     *      u2 bootstrap_arguments[num_bootstrap_arguments];
     *  } bootstrap_methods[num_bootstrap_methods];
     * }
     * </pre>
     */
    registerAttributeReader(
        BootstrapMethods_attributeReader.ATTRIBUTE_NAME -> (
            (ap: AttributeParent, cp: Constant_Pool, attribute_name_index: Constant_Pool_Index, in: DataInputStream) ⇒ {
                BootstrapMethods_attribute(
                    cp,
                    attribute_name_index,
                    in.readInt /* attribute_length */ ,
                    repeat(in.readUnsignedShort) { BootstrapMethod(cp, in) }
                )
            }
        )
    )
}

object BootstrapMethods_attributeReader {
    val ATTRIBUTE_NAME = "BootstrapMethods"
}

