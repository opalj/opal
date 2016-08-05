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

import scala.reflect.ClassTag

import java.io.DataInputStream
import org.opalj.control.repeat

/**
 * Generic parser for the ''ConcealedPackages'' attribute (Java 9).
 *
 * @author Michael Eichberg
 */
trait ConcealedPackages_attributeReader extends AttributeReader {

    type ConcealedPackages_attribute >: Null <: Attribute

    type ConcealedPackagesEntry
    implicit val ConcealedPackagesEntryManifest: ClassTag[ConcealedPackagesEntry]

    def ConcealedPackages_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        packages:             ConcealedPackages // basically a list of strings
    ): ConcealedPackages_attribute

    /**
     * @param packageIndex Points to the name in internal form of a package in
     * 		the current module that is ''not to be exported''.
     */
    def ConcealedPackagesEntry(
        constant_pool: Constant_Pool,
        packageIndex:  Constant_Pool_Index // CONSTANT_UTF8
    ): ConcealedPackagesEntry

    //
    // IMPLEMENTATION
    //

    type ConcealedPackages = IndexedSeq[ConcealedPackagesEntry]

    /**
     * <pre>
     * ConcealedPackages_attribute {
     *     u2 attribute_name_index;
     *     u4 attribute_length;
     *
     *     u2 packages_count;
     *     {   u2 package_index;
     *     } packages[package_count];
     * }
     * </pre>
     */
    private[this] def parser(
        ap:                   AttributeParent,
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in:                   DataInputStream
    ): ConcealedPackages_attribute = {
        /*val attribute_length = */ in.readInt()
        val packagesCount = in.readUnsignedShort()
        if (packagesCount > 0 || reifyEmptyAttributes) {
            ConcealedPackages_attribute(
                cp,
                attribute_name_index,
                repeat(packagesCount) {
                    ConcealedPackagesEntry(cp, in.readUnsignedShort)
                }
            )
        } else
            null
    }

    registerAttributeReader(ConcealedPackagesAttribute.Name → parser)
}

object ConcealedPackagesAttribute {

    final val Name = "ConcealedPackages"

}

