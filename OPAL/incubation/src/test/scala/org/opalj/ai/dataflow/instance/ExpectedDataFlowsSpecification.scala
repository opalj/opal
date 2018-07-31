/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow
package instance

import org.opalj.ai.dataflow.spec.SourcesAndSinks

/**
 * Enables the specification of `Sources` and `Sinks` between some tainted data flows.
 *
 * @author Michael Eichberg and Ben Hermann
 */
trait ExpectedDataFlowsSpecification {

    private[this] var thePaths: List[SourcesAndSinks] = Nil

    def paths(sourcesAndSinks: SourcesAndSinks): Unit = {
        thePaths = sourcesAndSinks :: thePaths
    }

    def paths: List[SourcesAndSinks] = thePaths
}

