/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

trait RegionType extends Product

trait AcyclicRegionType extends RegionType
trait CyclicRegionType extends RegionType

case object Block extends AcyclicRegionType
case object IfThen extends AcyclicRegionType
case object IfThenElse extends AcyclicRegionType
case object Case extends AcyclicRegionType
case object Proper extends AcyclicRegionType
case object SelfLoop extends CyclicRegionType
case object WhileLoop extends CyclicRegionType
case object NaturalLoop extends CyclicRegionType
case object Improper extends CyclicRegionType

case class Region(regionType: RegionType, nodeIds: Set[Int]) {

    override def toString: String = s"Region(${regionType.productPrefix}; ${nodeIds.toList.sorted.mkString(",")})"
}
