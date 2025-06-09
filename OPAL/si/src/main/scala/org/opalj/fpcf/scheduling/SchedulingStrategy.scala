package org.opalj.fpcf.scheduling

import org.opalj.fpcf.ComputationSpecification
import org.opalj.fpcf.PhaseConfiguration
import org.opalj.fpcf.PropertyStore

trait SchedulingStrategy[A] {
    /**
     * Schedules computations based on the strategy's specific algorithm.
     *
     * @param ps PropertyStore for the scheduling context
     * @param allCS Set of all ComputationSpecifications to be scheduled
     * @return List of PhaseConfiguration representing the scheduled phases
     */

    def schedule(
        ps:    PropertyStore,
        allCS: Set[ComputationSpecification[A]]
    ): List[PhaseConfiguration[A]]

    /**
     * Common helper method for computing a phase/batch.
     * Can be overridden by subclasses if needed.
     */
}
