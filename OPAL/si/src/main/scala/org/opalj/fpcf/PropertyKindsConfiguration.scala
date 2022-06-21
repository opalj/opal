/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

/**
 * Encapsulates the configuration of an analysis phase.
 *
 * @param propertyKindsComputedInThisPhase The set of properties which will be computed in the
 *        phase. Required to determine which properties will never be computed and for which –
 *        therefore – fallbacks should be used.
 *
 * @param propertyKindsComputedInLaterPhase The set of properties computed in a later phase.
 *        Used for debugging purposes!
 *
 * @param suppressInterimUpdates  Specifies for which properties updates no interim notifications
 *         shall be done. This ist generally only possible for those properties which are computed
 *         by analyses that do not take part in cyclic computations. For example,
 *         {{{
 *         ps.setupPhase(
 *           Set(ReachableNodes.Key, ReachableNodesCount.Key),
 *           Set.empty,
 *           Map(ReachableNodesCount.Key -> Set(ReachableNodes.Key))
 *         )
 *         }}}
 *         will suppress notifications about interim updates from `ReachableNodes` to
 *         `ReachableNodesCount`
 *
 *
 * @author Michael Eichberg
 */
case class PropertyKindsConfiguration(
        propertyKindsComputedInThisPhase:                      Set[PropertyKind],
        propertyKindsComputedInLaterPhase:                     Set[PropertyKind]                    = Set.empty,
        suppressInterimUpdates:                                Map[PropertyKind, Set[PropertyKind]] = Map.empty,
        collaborativelyComputedPropertyKindsFinalizationOrder: List[List[PropertyKind]]             = List.empty
) {

    override def toString: String = {
        s"PhaseConfiguration(\n\t"+
            propertyKindsComputedInThisPhase.map(PropertyKey.name).mkString("computedInThisPhase={", ", ", "}\n\t") +
            propertyKindsComputedInLaterPhase.map(PropertyKey.name).mkString("computedInLaterPhase={", ", ", "}\n\t") +
            suppressInterimUpdates.map {
                case (targetPK, sourcePKs) =>
                    PropertyKey.name(targetPK)+
                        "<-"+
                        sourcePKs.map(PropertyKey.name).mkString("{", ", ", "}")
            }.mkString("suppressInterimUpdates={", ",", "}\n\t") +
            collaborativelyComputedPropertyKindsFinalizationOrder
            .map(_.map(PropertyKey.name).mkString("[", ",", "]"))
            .mkString("finalizationOrder=[ ", " , ", " ]\n")+
            ")"
    }
}
