/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import org.opalj.log.OPALLogger
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject

/**
 * Configuration of a call graph algorithm that uses "variable type analysis".
 *
 * ==Thread Safety==
 * This class is thread-safe (it contains no mutable state.)
 *
 * ==Usage==
 * Instances of this class are passed to a `CallGraphFactory`'s `create` method.
 *
 * @author Michael Eichberg
 */
class CFACallGraphAlgorithmConfiguration(
        project: SomeProject,
        val k:   Int         = 2
) extends VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {

    import project.logContext

    CallGraphFactory.debug = true

    OPALLogger.info("progress", s"constructing a $k-CFA call graph")

    def Domain(method: Method) = {
        new CFACallGraphDomain(
            k,
            project, fieldValueInformation, methodReturnValueInformation,
            cache,
            method
        )
    }
}
