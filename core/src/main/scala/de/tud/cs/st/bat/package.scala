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
 * scalability and adaptability in mind. BAT's primary representation of Java byte code
 * is the [[de.tud.cs.st.bat.resolved]] representation which is defined in the
 * respective package.
 *
 * == This Package ==
 * Common constants and type definitions used across BAT.
 *
 * == General Design Rules ==
 * ===No `null` Values===
 * BAT does not make use of `null` values in its public interface. I.e., fields that
 * are accessible  will never contain `null` values and method methods will never
 * return `null`. If a method accepts `null` as a value for a parameter it is always
 * explicitly documented. In general, the behavior of methods that are passed `null`
 * values is undefined.
 *
 * ===Thread Safety===
 * Unless explicitly noted, '''BAT is thread safe'''. I.e., it is possible to read
 * and process class files concurrently without explicit synchronization on the
 * client side.
 *
 * @author Michael Eichberg
 */
package object bat {

    type AccessFlagsContext = AccessFlagsContexts.Value

    type AttributeParent = AttributesParent.Value

    type ConstantPoolTag = ConstantPoolTags.Value

    /**
     * Every Java class file start with "0xCAFEBABE".
     */
    final val CLASS_FILE_MAGIC = 0xCAFEBABE

}
