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
 * @param propertyKindsComputedInLaterPhase
 *
 * @param suppressInterimUpdates Specifies for which properties updates no interim notifications
 *        will be done. This ist generally only possible for those properties which are computed
 *        by analyses that do not take part in cyclic computations.
 */
case class PhaseConfiguration(
                                 propertyKindsComputedInThisPhase:  Set[PropertyKind],
                                 propertyKindsComputedInLaterPhase: Set[PropertyKind]                    = Set.empty,
                                 suppressInterimUpdates:            Map[PropertyKind, Set[PropertyKind]] = Map.empty
                             )
