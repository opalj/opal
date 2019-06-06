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

    def dropOldestTypes(seenElements: Int): Iterator[ObjectType]

    def numTypes: Int

    def types: UIDSet[ObjectType]

    def elements: PointsToSet

    def included(other: T): T
}
