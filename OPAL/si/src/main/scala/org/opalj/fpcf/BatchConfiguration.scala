/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf

import org.opalj.collection.immutable.Chain

case class BatchConfiguration[A](
                                    phaseConfiguration: PhaseConfiguration,
                                    batch:              Chain[ComputationSpecification[A]]
                                )
