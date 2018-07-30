/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow
package spec

/**
 * Support methods to facilitate the definition of data-flow constraints.
 *
 * @author Michael Eichberg and Ben Hermann
 */
abstract class DataFlowProblemSpecification[Source, P]
    extends DataFlowProblem[Source, P]
    with SourcesAndSinks {

    override def initializeSourcesAndSinks(): Unit = {
        initializeSourcesAndSinks(project)
    }

}

