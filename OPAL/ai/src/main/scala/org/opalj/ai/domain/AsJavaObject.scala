/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package ai
package domain

/**
 * Mixed in by domains that support the conversation of a `DomainValue` into
 * a respective Java object. This Java object can then be used to perform method
 * invocations.
 *
 * ==Limitation==
 * Using `AsJavaObject` will only work reasonably iff the respective class
 * is either in the classpath of the JVM or a class loader (initialized with the
 * project's classpath) is used.
 * The latter, however, does not work for classes on the bootclasspath (e.g.,
 * `java.lang.String`). In that case it is necessary to check that the code of the
 * analyzed application is compatible with the one on the class path.
 * '''To avoid accidental imprecision in the analysis you should use this features
 * only for stable classes belonging to the core JDK (`java.lang...`.)'''
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
trait AsJavaObject { domain: ReferenceValuesDomain ⇒

    /**
     * Converts – if possible – a given `DomainValue` to a Java object that is
     * appropriately initialized.
     *
     * ==Implementation==
     * Every domain that supports the creation of a Java object's based on a domain
     * value is expected to implement this method and to test if it can create
     * a precise representation of the given value. If not, the implementation has to delegate
     * the responsibility to the super method to creat an abstract representation.
     * {{{
     * abstract override def toJavaObject(value : DomainValue): Option[Object] = {
     *  if(value...)
     *      // create and return Java object
     *  else
     *      super.toJavaObject(value)
     * }
     * }}}
     *
     * @note   This operation is generally only possible if the domain value maintains
     *         ''enough'' state information to completely initialize the Java object.
     *
     * @return Some(Object) is returned if it was possible to create a compatible
     *         corresponding Java object; otherwise `None` is returned.
     *         Default: `None` unless the `value` is null. In the latter case `Some(null)`
     *         is returned.
     */
    def toJavaObject(pc: Int, value: DomainValue): Option[Object] = None

}
