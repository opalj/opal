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
package de.tud.cs

/**
 * OPAL is a Scala-based framework for the static analysis of Java bytecode
 * that is designed with performance, scalability and adaptability in mind.
 *
 * Its main components are:
 *  - a library for reading in Java bytecode and creating arbitrary representations
 *  - an easily customizable framework for the abstract interpretation of Java bytecode.
 *  - a library to extract dependencies between code elements. 
 *
 * ==General Design Decisions==
 *
 * ===Thread Safety===
 * Unless explicitly noted, '''OPAL is thread safe'''. I.e., it is possible to read
 * and process class files concurrently without explicit synchronization on the
 * client side.
 *
 * ===No `null` Values===
 * OPAL generally does not make use of `null` values in its public interface. I.e.,
 * fields that are accessible  will never contain `null` values and methods will never
 * return `null`. **If a method accepts `null` as a value for a parameter or returns
 * a `null` value it is always explicitly documented.**
 * In general, the behavior of methods that are passed `null` values is undefined.
 *
 * ===No Typecasts for Collections===
 * For efficiency reasons OPAL sometimes uses mutable data-structures during construction
 * time. After construction time, these data-structures are generally represented using
 * their generic type interfaces (`scala.collection.{Set,Map}`). However, a downcast
 * (e.g., to add elements) is always forbidden as it would effectively prevent the
 * thread-safety properties. Furthermore, the concrete data-structure is always
 * considered an implementation detail and may always change.
 *
 * @author Michael Eichberg
 */
package object st {

	final val WEBPAGE = "http://www.opal-project.de" 

    /**
     * A simple type alias that can be used to communicate that the respective
     * value will only take values in the range of unsigned short values.
     */
    type UShort = Int

}