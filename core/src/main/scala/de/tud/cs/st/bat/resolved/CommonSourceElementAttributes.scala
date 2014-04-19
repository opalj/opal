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
package de.tud.cs.st
package bat
package resolved

/**
 * Defines methods to return common attributes from the attributes table of
 * [[ClassFile]], [[Field]] and [[Method]] declarations.
 *
 * @author Michael Eichberg
 */
trait CommonSourceElementAttributes extends CommonAttributes {

    def runtimeVisibleAnnotations: Annotations =
        attributes collectFirst { case RuntimeVisibleAnnotationTable(vas) ⇒ vas } match {
            case Some(annotations) ⇒ annotations
            case None              ⇒ IndexedSeq.empty
        }

    def runtimeInvisibleAnnotations: Annotations =
        attributes collectFirst { case RuntimeInvisibleAnnotationTable(ias) ⇒ ias } match {
            case Some(annotations) ⇒ annotations
            case None              ⇒ IndexedSeq.empty
        }

    def annotations: Annotations =
        runtimeVisibleAnnotations ++ runtimeInvisibleAnnotations

    /**
     * True if this element was created by the compiler.
     */
    def isSynthetic: Boolean = attributes contains Synthetic

    /**
     * Returns true if this (field, method, class) declaration is declared
     * as deprecated.
     *
     * ==Note==
     * The deprecated attribute is always set by the Java compiler when either the
     * deprecated annotation or the JavaDoc tag is used.
     */
    def isDeprecated: Boolean = attributes contains Deprecated

}