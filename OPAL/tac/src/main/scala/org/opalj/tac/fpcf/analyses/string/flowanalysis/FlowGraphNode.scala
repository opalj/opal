/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

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

sealed trait FlowGraphNode {
    def nodeIds: Set[Int]
}

case class Region(regionType: RegionType, override val nodeIds: Set[Int]) extends FlowGraphNode {

    override def toString: String = s"Region(${regionType.productPrefix}; ${nodeIds.toList.sorted.mkString(",")})"
}

case class Statement(nodeId: Int) extends FlowGraphNode {
    override val nodeIds: Set[Int] = Set(nodeId)

    override def toString: String = s"Statement($nodeId)"
}
