/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties

import org.opalj.fpcf.FallbackReason
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyMetaInformation
import org.opalj.fpcf.PropertyStore
import org.opalj.br.collection.{TypesSet => BRTypesSet}

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
        ps:     PropertyStore,
        reason: FallbackReason,
        m:      br.Method
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
            fallbackPropertyComputation _
        )
    }

    final val NoExceptions = new ThrownExceptionsByOverridingMethods()

    final val SomeException = new ThrownExceptionsByOverridingMethods(BRTypesSet.SomeException)

    final val MethodIsOverridable =
        new ThrownExceptionsByOverridingMethods(BRTypesSet.SomeException)
}

case class ThrownExceptionsByOverridingMethods(
        exceptions: BRTypesSet = BRTypesSet.empty
) extends Property
    with ThrownExceptionsByOverridingMethodsPropertyMetaInformation {

    final def key = ThrownExceptionsByOverridingMethods.key

    override def toString: String = s"ThrownExceptionsByOverridingMethods(${exceptions.toString})"
}

