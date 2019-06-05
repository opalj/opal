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
trait PointsToSetLike[PointsToElements] extends Property {

    def dropOldest(seenElements: Int): Iterator[PointsToElements]

    def dropOldestTypes(seenElements: Int): Iterator[ObjectType]

    def numElements: Int

    def types: UIDSet[ObjectType]

    def updated(newElements: TraversableOnce[PointsToElements]): PointsToSetLike[PointsToElements]
}
