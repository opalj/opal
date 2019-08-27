/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package pointsto

import org.opalj.fpcf.Property

/**
 * TODO: Document
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
trait PointsToSetLike[ElementType, PointsToSet, T <: PointsToSetLike[ElementType, PointsToSet, T]] extends Property { self: T ⇒

    def forNewestNTypes[U](n: Int)(f: ReferenceType ⇒ U): Unit

    def numTypes: Int

    def types: Set[ReferenceType]

    def numElements: Int

    def elements: PointsToSet

    def forNewestNElements[U](n: Int)(f: ElementType ⇒ U): Unit

    def included(other: T): T

    def included(other: T, seenElements: Int): T

    def included(other: T, typeFilter: ReferenceType ⇒ Boolean): T

    def included(
        other: T, seenElements: Int, typeFilter: ReferenceType ⇒ Boolean
    ): T

    def includeOption(other: T): Option[T] = {
        val newSet = this.included(other)
        if (newSet eq this)
            None
        else
            Some(newSet)
    }

    def filter(typeFilter: ReferenceType ⇒ Boolean): T

    def getNewestElement(): ElementType
}

object PointsToSetLike {
    val noFilter = { t: ReferenceType ⇒ true }
}
