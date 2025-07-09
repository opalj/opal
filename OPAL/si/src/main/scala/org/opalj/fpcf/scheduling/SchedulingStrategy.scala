/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package scheduling

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
     * Computes the configuration for a specific batch; this method can only handle the situation
     * where all analyses can be executed in the same phase.
     */
    protected def computePhase(
        ps:                   PropertyStore,
        currentPhaseAnalyses: Set[ComputationSpecification[A]],
        nextPhaseAnalyses:    Set[ComputationSpecification[A]]
    ): PhaseConfiguration[A] = {

        // 1. compute the phase configuration; i.e., find those properties for which we must
        //    suppress interim updates.
        var suppressInterimUpdates: Map[PropertyKind, Set[PropertyKind]] = Map.empty
        // Interim updates have to be suppressed when an analysis uses a property for which
        // the wrong bounds/not enough bounds are computed.
        currentPhaseAnalyses foreach {
            case cs if cs.computationType == Transformer =>
                suppressInterimUpdates += (cs.derivesLazily.get.pk -> cs.uses(ps).map(_.pk))
        }

        def extractPropertyKinds(analyses: Set[ComputationSpecification[A]]): Set[PropertyKind] = {
            analyses.flatMap { analysis =>
                (analysis.derivesLazily.toSet ++
                    analysis.derivesEagerly ++
                    analysis.derivesCollaboratively ++
                    analysis.derives.toSet)
                    .map(_.pk)
            }
        }

        val propertyKindsFromPhaseAnalysis = extractPropertyKinds(currentPhaseAnalyses)
        val propertyKindsFromNextPhaseAnalysis = extractPropertyKinds(nextPhaseAnalyses)

        val collabProperties = currentPhaseAnalyses.flatMap { analysis => analysis.derivesCollaboratively.map(_.pk) }

        // 3. create the batch
        val batchBuilder = List.newBuilder[ComputationSpecification[A]]
        batchBuilder ++= currentPhaseAnalyses

        // FIXME...

        // Interim updates can be suppressed when the depender and dependee are not in a cyclic
        // relation; however, this could have a negative impact on the effect of deep laziness -
        // once we are actually implementing it. For the time being, suppress notifications is always
        // advantageous.

        val phase1Configuration = PropertyKindsConfiguration(
            propertyKindsComputedInThisPhase = propertyKindsFromPhaseAnalysis,
            suppressInterimUpdates = suppressInterimUpdates,
            propertyKindsComputedInLaterPhase = propertyKindsFromNextPhaseAnalysis,
            collaborativelyComputedPropertyKindsFinalizationOrder = List(collabProperties.toList) // FIXME: Compute actual subphase finalization order
        )

        PhaseConfiguration(phase1Configuration, batchBuilder.result())
    }
}
