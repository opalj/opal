/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf
package fixtures

/**
 * Models a Property which counts something. For example, the set of reachable nodes.
 *
 * @note Only intended to be used as a test fixture.
 */
object ReachableNodesCount {

    val Key: PropertyKey[ReachableNodesCount] =
        PropertyKey.create[Node, ReachableNodesCount](
            s"ReachableNodesCount",
            (_: PropertyStore, _: FallbackReason, _: Node) => TooManyNodesReachable
        )
}

case class ReachableNodesCount(value: Int) extends OrderedProperty {
    type Self = ReachableNodesCount
    def key: PropertyKey[ReachableNodesCount] = ReachableNodesCount.Key

    def checkIsEqualOrBetterThan(e: Entity, other: ReachableNodesCount): Unit = {
        if (this.value > other.value) {
            throw new IllegalArgumentException(s"$e: $this is not equal or better than $other")
        }
    }
}

object NoNodesReachable extends ReachableNodesCount(0) {
    override def toString: String = "NoNodesReachable"
}
object TooManyNodesReachable extends ReachableNodesCount(64) {
    override def toString: String = "TooManyNodesReachable"
}
