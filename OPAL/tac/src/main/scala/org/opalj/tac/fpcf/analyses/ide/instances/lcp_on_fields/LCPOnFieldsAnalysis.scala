/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields

import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaIDEAnalysis

/**
 * Linear constant propagation on fields as IDE analysis. This implementation is mainly intended to be an example of a
 * cyclic IDE analysis (see [[LCPOnFieldsProblem]]).
 */
class LCPOnFieldsAnalysis(project: SomeProject)
    extends JavaIDEAnalysis(
        project,
        new LCPOnFieldsProblem(project),
        LCPOnFieldsPropertyMetaInformation
    ) {
    val lcpOnFieldsProblem: LCPOnFieldsProblem =
        problem.asInstanceOf[LCPOnFieldsProblem]
}
