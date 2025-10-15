/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package si
package flowanalysis

import scala.util.hashing.MurmurHash3

sealed trait RegionType extends Product

sealed trait AcyclicRegionType extends RegionType
sealed trait CyclicRegionType extends RegionType

case object Block extends AcyclicRegionType
case object IfThen extends AcyclicRegionType
case object IfThenElse extends AcyclicRegionType
case object Case extends AcyclicRegionType
case object Proper extends AcyclicRegionType
case object SelfLoop extends CyclicRegionType
case object WhileLoop extends CyclicRegionType
case object NaturalLoop extends CyclicRegionType
case object Improper extends CyclicRegionType

/**
 * A node in a flow graph and control tree produced by the [[StructuralAnalysis]].
 *
 * @author Maximilian Rüsch
 */
sealed trait FlowGraphNode extends Ordered[FlowGraphNode] {

    def nodeIds: Set[Int]

    override def compare(that: FlowGraphNode): Int = nodeIds.toList.min.compare(that.nodeIds.toList.min)
}

/**
 * Represents a region of nodes in a [[FlowGraph]], consisting of multiple sub-nodes. Can identify general acyclic and
 * cyclic structures or more specialised instances of such structures such as [[IfThenElse]] or [[WhileLoop]].
 *
 * @param regionType The type of the region.
 * @param nodeIds The union of all ids the leafs that are contained in this region.
 * @param entry The direct child of this region that contains the first leaf to be executed when entering the region.
 */
case class Region(regionType: RegionType, override val nodeIds: Set[Int], entry: FlowGraphNode) extends FlowGraphNode {

    override def toString: String =
        s"Region(${regionType.productPrefix}; ${nodeIds.toList.sorted.mkString(",")}; ${entry.toString})"

    // Performance optimizations
    private lazy val _hashCode = MurmurHash3.productHash(this)
    override def hashCode(): Int = _hashCode
    override def canEqual(obj: Any): Boolean = obj.hashCode() == _hashCode
}

/**
 * Represents a single statement in a methods [[FlowGraph]] and is one of the leaf nodes to be grouped by a [[Region]].
 *
 * @param pc The PC that the statement is given at.
 */
case class Statement(pc: Int) extends FlowGraphNode {

    override val nodeIds: Set[Int] = Set(pc)

    override def toString: String = s"Statement($pc)"
}

/**
 * An additional global entry node to a methods [[FlowGraph]] to ensure only one entry node exists.
 */
object GlobalEntry extends FlowGraphNode {

    override val nodeIds: Set[Int] = Set(Int.MinValue + 1)

    override def toString: String = "GlobalEntry"
}

/**
 * An additional global exit node to a methods [[FlowGraph]] to ensure only one exit node exists.
 */
object GlobalExit extends FlowGraphNode {

    override val nodeIds: Set[Int] = Set(Int.MinValue)

    override def toString: String = "GlobalExit"
}
