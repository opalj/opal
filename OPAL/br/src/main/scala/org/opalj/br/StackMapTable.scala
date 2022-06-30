/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.collection.immutable.IntArraySet

/**
 * Java 6's stack map table attribute.
 *
 * @author Michael Eichberg
 */
case class StackMapTable(stackMapFrames: StackMapFrames) extends Attribute {

    override def kindId: Int = StackMapTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }

    /**
     * The pcs of instructions with a stack map frame.
     *
     * @return The program counters of those instructions for which we have an explicit stack map
     *         frame.
     */
    def pcs: IntArraySet = {
        var pcs = IntArraySet.empty
        var previousOffset: Int = -1
        stackMapFrames.foreach { f =>
            previousOffset = f.offset(previousOffset)
            pcs += previousOffset
        }

        pcs
    }

}
object StackMapTable {

    final val KindId = 7

}
