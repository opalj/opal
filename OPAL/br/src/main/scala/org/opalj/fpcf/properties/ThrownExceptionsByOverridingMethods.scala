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
package fpcf
package properties

import org.opalj.br.collection.{TypesSet ⇒ BRTypesSet}

sealed trait ThrownExceptionsByOverridingMethodsPropertyMetaInformation
    extends PropertyMetaInformation {

    final type Self = ThrownExceptionsByOverridingMethods

}

/**
 * The set of exceptions thrown by a method, including the exceptions thrown by overriding methods.
 * If the type hierarchy is extensible then the set is generally unbounded.
 *
 * Information about `ThrownExceptionsByOverridingMethods` is generally associated with
 * `DeclaredMethods`. I.e., the information is not attached to `Method` objects!
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object ThrownExceptionsByOverridingMethods
    extends ThrownExceptionsByOverridingMethodsPropertyMetaInformation {
    def apply(exceptions: BRTypesSet = BRTypesSet.empty): ThrownExceptionsByOverridingMethods =
        new ThrownExceptionsByOverridingMethods(exceptions)

    def fallbackPropertyComputation(
        ps: PropertyStore,
        m:  br.Method
    ): ThrownExceptionsByOverridingMethods = {
        if (m.isFinal || m.isStatic || m.isInitializer || m.isPrivate) {
            new ThrownExceptionsByOverridingMethods(ThrownExceptionsFallback(ps, m).types)
        } else {
            SomeException
        }
    }

    final val key: PropertyKey[ThrownExceptionsByOverridingMethods] = {
        PropertyKey.create[br.Method, ThrownExceptionsByOverridingMethods](
            name = "ThrownExceptionsByOverridingMethods",
            fallbackPropertyComputation _,
            (_: PropertyStore, eps: EPS[br.Method, ThrownExceptionsByOverridingMethods]) ⇒
                eps.toUBEP
        )
    }

    final val NoExceptions = new ThrownExceptionsByOverridingMethods()

    final val SomeException = new ThrownExceptionsByOverridingMethods(BRTypesSet.SomeException)

    final val MethodIsOverridable =
        new ThrownExceptionsByOverridingMethods(BRTypesSet.SomeException)
}

case class ThrownExceptionsByOverridingMethods(
        exceptions: BRTypesSet = BRTypesSet.empty
) extends Property with ThrownExceptionsByOverridingMethodsPropertyMetaInformation {

    final def key = ThrownExceptionsByOverridingMethods.key

    override def toString: String = s"ThrownExceptionsByOverridingMethods(${exceptions.toString})"
}

