/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package pointsto
package properties

import org.opalj.fpcf.Property

/**
 * TODO: Document
 *
 * @author Dominik Helm
 * @author Florian Kuebler
 */
trait PointsToSetLike extends Property {

    def dropOldestTypes(seenElements: Int): Iterator[ObjectType]

    def numElements: Int
}
