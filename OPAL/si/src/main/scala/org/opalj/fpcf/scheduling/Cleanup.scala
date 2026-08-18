/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

import scala.annotation.targetName

/**
 * Class that allows to configure the cleanup of the PropertyStore inbetween phases programmatically
 *
 * @param keep IDs of the PropertyKeys to be kept at the end
 * @param clear IDs of the PropertyKeys to be definitely removed
 * @param enable Allows the cleanup to be enabled since it is off by default
 */
final case class CleanupSpec(
    keep:   Set[Int] = Set.empty,
    clear:  Set[Int] = Set.empty,
    enable: Boolean  = false
)

/**
 * Companion Object
 */
object CleanupSpec {
    /**
     * Creates a [[CleanupSpec]] from given [[PropertyKey]]-names to keep and/or to clear. Also allows enabling the cleanup by setting 'enable' to 'true'.
     *
     * @param keep   Names of PropertyKeys to be kept after the analyses
     * @param clear  Names of PropertyKeys to be removed after the analyses
     * @param enable Setting this to 'true' enables the cleanup inbetween the phases
     * @return A new [[CleanupSpec]]
     */
    def apply(keep: Set[String], clear: Set[String], enable: Boolean)(implicit di: DummyImplicit): CleanupSpec = {
        val toKeep = keep.map(PropertyKey.idByName)
        val toClear = clear.map(PropertyKey.idByName)
        CleanupSpec(toKeep, toClear, enable)
    }

}

/**
 * Handles calculation of per-phase cleanup
 */
object Cleanup {

    /**
     * Calculates the properties to be safely removed inbetween phases. Returns an unmodified schedule if cleanup is disabled
     */
    def withPerPhaseCleanup[A](
        schedule: List[PhaseConfiguration[A]],
        ps:       PropertyStore,
        spec:     CleanupSpec
    ): List[PhaseConfiguration[A]] = {
        // in case the cleanup is disabled, we don't want to modify the schedules per-phase "toDelete"
        if (!spec.enable) return schedule

        // calculate the pks which are produced in any phase (so we can ignore the rest)
        val producedInAnyPhase: Set[Int] =
            schedule.iterator.flatMap(_.propertyKinds.propertyKindsComputedInThisPhase.map(_.id)).toSet
        val neededLater = Array.fill[Set[Int]](schedule.size + 1)(Set.empty)

        var index = schedule.size - 1
        var usedInAnyPhase: Set[Int] = Set.empty

        // calculate neededLater and usedInAnyPhase iteratively while keeping neededLater minimal at each time
        //
        while (index >= 0) {
            val usedInThisPhase: Set[Int] =
                schedule(index).scheduled.iterator.flatMap(_.uses(ps).iterator).map(_.pk.id).toSet
            neededLater(index) = usedInAnyPhase
            usedInAnyPhase = usedInAnyPhase union usedInThisPhase
            index -= 1
        }

        // store to keep track of already deleted pks to not delete them again in later phases
        var alreadyCleared = Set.empty[Int]
        val lastIndex = schedule.size - 1

        // calculate the pks to be deleted after each phase
        schedule.indices.iterator.map { index =>
            val producedHere = schedule(index)

            // only delete the pks the user wants to delete as it's the final phase.
            // Anything not deleted will be returned
            val toDelete = if (index == lastIndex) {
                spec.clear -- alreadyCleared
            } else {
                // delete any pk safe to delete (not needed later) which isn't already deleted
                // we also don't want to delete anything the user wants to keep
                (((producedInAnyPhase -- neededLater(index)) -- alreadyCleared) -- spec.keep) union spec.clear
            }
            // keep track of the deleted pks
            alreadyCleared ++= toDelete
            producedHere.copy(toDelete = toDelete)
        }.toList
    }

}
