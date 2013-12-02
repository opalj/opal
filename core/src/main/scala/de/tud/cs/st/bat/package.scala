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

/**
 * BAT is a generic Java bytecode library written in Scala that is designed with
 * scalability and adaptability in mind.
 *
 * == This Package ==
 * Common constants and type definitions used across BAT.
 *
 * == General Design Rules ==
 * ===No `null` Values===
 * BAT does not make use of `null` values in its public interface. I.e., fields 
 * will never contain `null` values,
 * method parameters must not be `null` and methods will never return `null`
 * values.
 *
 * ===Thread Safety===
 * Unless explicitly noted, BAT is thread safe. I.e., it is possible to read and process
 * class files in parallel without explicit synchronization on the client side.
 *
 * @author Michael Eichberg
 */
package object bat {

    type AccessFlagsContext = AccessFlagsContexts.Value

    type AttributeParent = AttributesParent.Value

    type ConstantPoolTag = ConstantPoolTags.Value

    final val CLASS_FILE_MAGIC = 0xCAFEBABE

    final val generalBATExceptionMessage: String =
        "An internal error occured while reading/analyzing a class file: "

    @throws[BATException]
    final def BATException(message: String): Nothing =
        throw new BATException(generalBATExceptionMessage + message)

    //
    // Common matchers for access flags
    // Use:
    // case Method(PUBLIC_STATIC(),...) =>
    // case Field(PUBLIC_STATIC_FINAL(),...) =>
    //
    val PUBLIC_INTERFACE = ACC_PUBLIC & ACC_INTERFACE
    val PUBLIC_ABSTRACT = ACC_PUBLIC & ACC_ABSTRACT
    val PUBLIC_FINAL = ACC_PUBLIC & ACC_FINAL
    val PUBLIC_STATIC = ACC_PUBLIC & ACC_STATIC
    val PUBLIC_STATIC_FINAL = PUBLIC_FINAL & ACC_STATIC
    val NOT_INTERFACE = !ACC_INTERFACE
    val NOT_STATIC = !ACC_STATIC
    val NOT_PRIVATE = !ACC_PRIVATE
    val NOT_FINAL = !ACC_FINAL
    val NOT_SYNCHRONIZED = !ACC_SYNCHRONIZED
    val NOT_NATIVE = !ACC_NATIVE
    val NOT_ABSTRACT = !ACC_ABSTRACT

}
