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
trait PointsToSetLike[ElementType] extends Property {

    type Self <: PointsToSetLike[ElementType]

    def dropOldestTypes(seenElements: Int): Iterator[ObjectType]

    def numTypes: Int

    def types: UIDSet[ObjectType]

    def included(other: Self): Self

    def included(newElements: TraversableOnce[ElementType]): Self
}
