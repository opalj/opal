package org.opalj
package fpcf
package scheduling

/**
 * Class that allows to configure the cleanup of the PropertyStore inbetween phases programmatically
 * @param keep IDs of the PropertyKeys to be kept at the end
 * @param clear IDs of the PropertyKeys to be definitely removed
 * @param disable Allows the cleanup to be disabled since it is on by default
 */
final case class CleanupSpec(
    keep:    Set[Int] = Set.empty,
    clear:   Set[Int] = Set.empty,
    disable: Boolean  = false
)

object Cleanup {

    /**
     * Creates a [[CleanupSpec]] from given [[PropertyKey]]-names to keep and/or to clear. Also allows disabling the cleanup by setting 'disable' to 'true'.
     * @param keep Names of PropertyKeys to be kept after the analyses
     * @param clear Names of PropertyKeys to be removed after the analyses
     * @param disable Setting this to 'true' disables the cleanup inbetween the phases
     * @return A new [[CleanupSpec]]
     */
    def fromArgs(keep: Set[String], clear: Set[String], disable: Boolean): CleanupSpec = {
        val toKeep = keep.map(PropertyKey.idByName)
        val toClear = clear.map(PropertyKey.idByName)
        CleanupSpec(toKeep, toClear, disable)
    }

    /**
     * Calculates the properties to be safely removed inbetween phases. Returns an unmodified schedule if cleanup is disabled
     */
    def withPerPhaseCleanup[A](
        schedule: List[PhaseConfiguration[A]],
        ps:       PropertyStore,
        spec:     CleanupSpec
    ): List[PhaseConfiguration[A]] = {
        if (spec.disable) return schedule

        val producedInAnyPhase: Set[Int] =
            schedule.iterator.flatMap(_.propertyKinds.propertyKindsComputedInThisPhase.map(_.id)).toSet
        val neededLater = Array.fill[Set[Int]](schedule.size + 1)(Set.empty)
        var index = schedule.size - 1
        var usedInAnyPhase: Set[Int] = Set.empty
        while (index >= 0) {
            val usedInThisPhase: Set[Int] =
                schedule(index).scheduled.iterator.flatMap(_.uses(ps).iterator).map(_.pk.id).toSet
            usedInAnyPhase = usedInAnyPhase union usedInThisPhase
            neededLater(index) = usedInAnyPhase
            index -= 1
        }

        schedule.indices.iterator.map { index =>
            val producedHere = schedule(index)
            val toDelete = ((producedInAnyPhase -- neededLater(index)) -- spec.keep) union spec.clear
            producedHere.copy(toDelete = toDelete)
        }.toList
    }

}
