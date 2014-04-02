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
 * The runtime (in)visible annotations of a class, method, or field.
 *
 * @note At the JVM level, repeating annotations 
 *    ([[http://docs.oracle.com/javase/tutorial/java/annotations/repeating.html]])
 *    have no explicit support.
 *    For further information about type-level annotations go to: 
 *    [[http://cr.openjdk.java.net/~abuckley/8misc.pdf]].
 * @author Michael Eichberg
 */
trait AnnotationTable extends Attribute {

    /**
     * Returns true if these annotations are visible at runtime.
     */
    def isRuntimeVisible: Boolean

    /**
     * The set of declared annotations; it may be empty.
     */
    def annotations: Annotations

}

/**
 * Functionality common to annotation tables.
 *
 * @author Michael Eichberg
 */
object AnnotationTable {

    def unapply(aa: AnnotationTable): Option[(Boolean, Annotations)] =
        Some(aa.isRuntimeVisible, aa.annotations)
}
