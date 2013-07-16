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
 * BAT is a Java bytecode library written in Scala.
 *
 * == This Package ==
 * Common constants and type definitions used across BAT.
 *
 * == General Design Rules ==
 * Unless explicitly noted, BAT does not make use of `null` values. I.e., method parameters must not be
 * `null` methods and will never return `null` values.
 *
 * Unless explicitly noted, BAT is thread safe. I.e., it is possible to read and process class files
 * in parallel.
 *
 * @author Michael Eichberg
 */
package object bat {

    type AccessFlagsContext = AccessFlagsContexts.Value

    type AttributeParent = AttributesParent.Value

    type ConstantPoolTag = ConstantPoolTags.Value

    final val CLASS_FILE_MAGIC = 0xCAFEBABE

    private final val generalBATErrorMessage: String =
        """While reading a class file an unexpected error occured.
          |Either the class file is corrupt or an internal error was found.
          |The underlying problem is:
          |""".stripMargin('|')

    @throws[RuntimeException]
    final def BATError(message: String) = throw new RuntimeException(generalBATErrorMessage + message)

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
    val NOT_STATIC = !ACC_STATIC
    val NOT_PRIVATE = !ACC_PRIVATE
    val NOT_FINAL = !ACC_FINAL
}