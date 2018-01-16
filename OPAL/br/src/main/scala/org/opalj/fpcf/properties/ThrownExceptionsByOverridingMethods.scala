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

import org.opalj.br.ObjectType
import org.opalj.br.collection.{TypesSet ⇒ BRTypesSet}
import org.opalj.br.collection.UpperTypeBounds

/**
 * The set of exceptions thrown by a method, including the exceptions including the exceptions
 * thrown by overriding methods, if the set of overriding methods is finite.
 *
 * It uses the ThrownExceptions property to gather information about the exceptions thrown by a
 * particular method.
 *
 * @author Andreas Muttscheller
 * @author Michael Eichberg
 */
object ThrownExceptionsByOverridingMethods {

    private[this] final val cycleResolutionStrategy = {
        (ps: PropertyStore, eps: SomeEPS) ⇒ { Result(eps.e, eps.p) }: Result
    }

    final val Key: PropertyKey[ThrownExceptionsByOverridingMethods] = {
        PropertyKey.create[ThrownExceptionsByOverridingMethods](
            "ThrownExceptionsByOverridingMethods",
            Unknown,
            cycleResolutionStrategy
        )

    }

    final val NoExceptions = new ThrownExceptionsByOverridingMethods()

    final val Unknown =
        new ThrownExceptionsByOverridingMethods(
            exceptions = UpperTypeBounds(Set(ObjectType.Throwable))
        )
}

case class ThrownExceptionsByOverridingMethods(
        exceptions: BRTypesSet = BRTypesSet.empty
) extends Property {
    final type Self = ThrownExceptionsByOverridingMethods
    final def key = ThrownExceptionsByOverridingMethods.Key
}

