/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package properties
package pointsto

import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.Property

/**
 * TODO: Document
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
trait PointsToSetLike[ElementType, PointsToSet, T <: PointsToSetLike[ElementType, PointsToSet, T]] extends Property { self: T ⇒

    def dropOldestTypes(seenTypes: Int): Iterator[ObjectType]

    def numTypes: Int

    def types: UIDSet[ObjectType]

    def numElements: Int

    def elements: PointsToSet

    def dropOldestElements(seenElements: Int): Iterator[ElementType]

    def included(other: T): T

    def included(other: T, seenElements: Int): T

    def includeOption(other: T): Option[T] = {
        val newSet = this.included(other)
        if (newSet eq this)
            None
        else
            Some(newSet)
    }
}
