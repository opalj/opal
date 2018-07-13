/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package dataflow

import org.opalj.br.analyses.Project

trait DataFlowProblemFactory {

    /*abstract*/ type P

    /*abstract*/ def description: String

    /*abstract*/ def title: String

    /*abstract*/ def processAnalysisParameters(parameters: Seq[String]): P

    /*abstract*/ def create[Source](
        project: Project[Source],
        p:       P
    ): DataFlowProblem[Source, P]
}
