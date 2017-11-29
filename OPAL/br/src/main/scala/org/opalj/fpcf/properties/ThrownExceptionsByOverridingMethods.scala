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
import org.opalj.br.collection.mutable.{TypesSet ⇒ BRMutableTypesSet}
import org.opalj.br.analyses.SomeProject

/**
 * This property stores information about the exceptions a certain method throw, including
 * the exceptions a possible overridden method in a subclass throws.
 * It uses the ThrownExceptions property to gather information about the exceptions thrown in a
 * particular method. It also includes the thrown exceptions of the respective method in all
 * subclasses.
 *
 * Results can either be `AllThrownExceptionsByOverridingMethods`, which contains a set of possible
 * exceptions thrown in the current classes method or its subclasses. If we aren't able to collect
 * all exceptions, `UnknownThrownExceptionsByOverridingMethods` will be returned. This is the case
 * if the analysis encounters a ATHROW instruction for example.
 *
 * The cycle resolution collects the properties from the given entities and constructs a final
 * result. Possible properties can be `ThrownExceptionsByOverridingMethods` as well as
 * `ThrownExceptions`. The result will be saved in the PropertyStore and propagated to the dependees.
 */
object ThrownExceptionsByOverridingMethods {

    private[this] final val cycleResolutionStrategy = (
        ps: PropertyStore,
        epks: Iterable[SomeEPK]
    ) ⇒ {
        val exceptions = new BRMutableTypesSet(ps.context[SomeProject].classHierarchy)
        var hasUnknownExceptions = false
        epks.foreach {
            case EPK(e, Key) ⇒
                ps(e, Key).p match {
                    case c: AllThrownExceptionsByOverridingMethods ⇒
                        exceptions ++= c.exceptions.concreteTypes
                    case UnknownThrownExceptionsByOverridingMethods ⇒
                        hasUnknownExceptions = true
                    case _ ⇒ throw new UnknownError(s"Cycle involving unknown keys: $e")
                }

            case EPK(e, ThrownExceptions.Key) ⇒
                ps(e, ThrownExceptions.Key).p match {
                    case _: ThrownExceptionsAreUnknown ⇒ hasUnknownExceptions = true
                    case t: AllThrownExceptions        ⇒ exceptions ++= t.types.concreteTypes
                }
        }
        val entity = epks.find(_.pk == Key).get.e
        val p = if (hasUnknownExceptions)
            UnknownThrownExceptionsByOverridingMethods
        else
            AllThrownExceptionsByOverridingMethods(exceptions)

        Iterable(Result(entity, p))
    }

    final val Key: PropertyKey[ThrownExceptionsByOverridingMethods] = {
        PropertyKey.create[ThrownExceptionsByOverridingMethods](
            "ThrownExceptionsByOverridingMethodsProperty",
            AllThrownExceptionsByOverridingMethods(),
            cycleResolutionStrategy
        )
    }
}

sealed abstract class ThrownExceptionsByOverridingMethods extends Property {
    final type Self = ThrownExceptionsByOverridingMethods
    final def key = ThrownExceptionsByOverridingMethods.Key
}

case class AllThrownExceptionsByOverridingMethods(
        exceptions:   BRTypesSet = BRTypesSet.empty,
        isRefineable: Boolean    = false
) extends ThrownExceptionsByOverridingMethods {

    override def equals(other: Any): Boolean = {
        other match {
            case that: AllThrownExceptionsByOverridingMethods ⇒
                this.isRefineable == that.isRefineable &&
                    this.exceptions == that.exceptions
            case _ ⇒ false
        }
    }

    override def hashCode: Int = 13 * exceptions.hashCode +
        (if (isRefineable) 41 else 53)
}

case object UnknownThrownExceptionsByOverridingMethods
    extends ThrownExceptionsByOverridingMethods {
    final val isRefineable = false
}
