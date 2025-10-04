package org.opalj
package fpcf
package scheduling

import com.typesafe.config.Config

import org.opalj.fpcf.scheduling.CleanupCalculation.DisableCleanupKey
import org.opalj.fpcf.scheduling.CleanupCalculation.PropertiesToKeepKey
import org.opalj.fpcf.scheduling.CleanupCalculation.PropertiesToRemoveKey

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
     * Creates a [[CleanupSpec]] reading the optionally set Values from a given config.
     * @param config Config to be read from
     * @return A new [[CleanupSpec]]
     */
    def fromConfig(config: Config): CleanupSpec = {
        def getSetForProperty(propertyKey: String): Set[Int] = {
            Option(
                config.getString(propertyKey)
            ).toSeq.flatMap(_.split(",")).filter(_.nonEmpty).map(PropertyKey.idByName).toSet
        }
        val toKeep = getSetForProperty(PropertiesToKeepKey)
        val toClear = getSetForProperty(PropertiesToRemoveKey)
        val disable = config.getBoolean(DisableCleanupKey)
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

        val producedInAllPhases: Set[Int] =
            schedule.iterator.flatMap(_.propertyKinds.propertyKindsComputedInThisPhase.map(_.id)).toSet
        val neededLater = Array.fill[Set[Int]](schedule.size + 1)(Set.empty)
        var index = schedule.size - 1
        var acc: Set[Int] = Set.empty
        while (index >= 0) {
            val used: Set[Int] = schedule(index).scheduled.iterator.flatMap(_.uses(ps).iterator).map(_.pk.id).toSet
            acc = acc union used
            neededLater(index) = acc
            index -= 1
        }

        schedule.indices.iterator.map { index =>
            val producedHere = schedule(index)
            val toDelete = ((producedInAllPhases -- neededLater(index)) -- spec.keep) union spec.clear
            producedHere.copy(toDelete = toDelete)
        }.toList
    }

}
