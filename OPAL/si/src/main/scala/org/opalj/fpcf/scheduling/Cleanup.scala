/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

/**
 * Class that allows to configure the cleanup of the PropertyStore inbetween phases programmatically
 * @param keep IDs of the PropertyKeys to be kept at the end
 * @param clear IDs of the PropertyKeys to be definitely removed
 * @param enable Allows the cleanup to be enabled since it is off by default
 */
final case class CleanupSpec(
    keep:                  Set[Int] = Set.empty,
    clear:                 Set[Int] = Set.empty,
    enable:                Boolean  = false,
    cleanupAfterLastPhase: Boolean  = false
)

/**
 * Factory for creating [[CleanupSpec]]-objects, also handles calculation of per-phase cleanup
 */
object Cleanup {

    /**
     * Creates a [[CleanupSpec]] from given [[PropertyKey]]-names to keep and/or to clear. Also allows disabling the cleanup by setting 'disable' to 'true'.
     * @param keep Names of PropertyKeys to be kept after the analyses
     * @param clear Names of PropertyKeys to be removed after the analyses
     * @param enable Setting this to 'true' enables the cleanup inbetween the phases
     * @return A new [[CleanupSpec]]
     */
    def fromArgs(keep: Set[String], clear: Set[String], enable: Boolean): CleanupSpec = {
        val toKeep = keep.map(PropertyKey.idByName)
        val toClear = clear.map(PropertyKey.idByName)
        CleanupSpec(toKeep, toClear, enable)
    }

    /**
     * Calculates the properties to be safely removed inbetween phases. Returns an unmodified schedule if cleanup is disabled
     */
    def withPerPhaseCleanup[A](
        schedule: List[PhaseConfiguration[A]],
        ps:       PropertyStore,
        spec:     CleanupSpec
    ): List[PhaseConfiguration[A]] = {
        if (!spec.enable) return schedule

        val producedInAnyPhase: Set[Int] =
            schedule.iterator.flatMap(_.propertyKinds.propertyKindsComputedInThisPhase.map(_.id)).toSet
        val neededLater = Array.fill[Set[Int]](schedule.size + 1)(Set.empty)
        var index = schedule.size - 1
        var usedInAnyPhase: Set[Int] = Set.empty
        while (index >= 0) {
            val usedInThisPhase: Set[Int] =
                schedule(index).scheduled.iterator.flatMap(_.uses(ps).iterator).map(_.pk.id).toSet
            neededLater(index) = usedInAnyPhase
            usedInAnyPhase = usedInAnyPhase union usedInThisPhase
            index -= 1
        }

        var alreadyCleared = Set.empty[Int]
        val lastIndex = schedule.size - 1
        schedule.indices.iterator.map { index =>
            val producedHere = schedule(index)
            val toDelete = if (index == lastIndex) {
                spec.clear -- alreadyCleared
            } else {
                (((producedInAnyPhase -- neededLater(index)) -- alreadyCleared) -- spec.keep) union spec.clear
            }

            alreadyCleared ++= toDelete
            producedHere.copy(toDelete = toDelete)
        }.toList
    }

}
