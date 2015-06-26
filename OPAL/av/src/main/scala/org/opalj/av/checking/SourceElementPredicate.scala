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
package av
package checking

import org.opalj.br.ConcreteSourceElement
import org.opalj.bi.AccessFlagsMatcher
import org.opalj.br.{ Attributes ⇒ SourceElementAttributes }

/**
 * A predicate related to a specific source element.
 *
 * @author Michael Eichberg
 */
trait SourceElementPredicate[-S <: ConcreteSourceElement] extends (S ⇒ Boolean) { left ⇒

    def and[T <: ConcreteSourceElement, X <: S with T](
        right: SourceElementPredicate[T]): SourceElementPredicate[X] = {

        new SourceElementPredicate[X] {
            def apply(s: X): Boolean = left(s) && right(s)
            def toDescription() = left.toDescription()+" and "+right.toDescription()
        }

    }

    final def having[T <: ConcreteSourceElement, X <: S with T](
        right: SourceElementPredicate[T]): SourceElementPredicate[X] = and(right)

    /**
     * Returns a human readable representation of this predicate that is well suited
     * for presenting it in messages related to architectural deviations.
     *
     * It should not end with a white space and should not use multiple lines.
     * It should not be a complete sentence as this description may be composed with
     * other descriptions.
     */
    def toDescription(): String
}

case class AccessFlags(
    accessFlags: AccessFlagsMatcher)
        extends SourceElementPredicate[ConcreteSourceElement] {

    def apply(sourceElement: ConcreteSourceElement): Boolean = {
        this.accessFlags.unapply(sourceElement.accessFlags)
    }

    def toDescription(): String = accessFlags.toString

}

/**
 * @author Marco Torsello
 * @author Michael Eichberg
 */
case class Attributes(
    attributes: SourceElementAttributes)
        extends SourceElementPredicate[ConcreteSourceElement] {

    def apply(sourceElement: ConcreteSourceElement): Boolean = {
        //    sourceElement.attributes.size == this.attributes.size &&
        this.attributes.forall(a ⇒ sourceElement.attributes.exists(_ == a))
    }

    def toDescription(): String = {
        attributes.view.map(_.getClass.getSimpleName).mkString(" « ", ", ", " »")
    }
}
