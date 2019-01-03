/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.collection.immutable.Chain

case class BatchConfiguration[A]( // TODO rename PhaseConfiguration (after renaming PhaseConfiguration)
        phaseConfiguration: PropertyKindsConfiguration,
        batch:              Chain[ComputationSpecification[A]] // TODO rename phase
)
