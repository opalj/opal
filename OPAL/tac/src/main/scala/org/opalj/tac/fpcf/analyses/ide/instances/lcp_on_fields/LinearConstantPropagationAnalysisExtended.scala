/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields

import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LinearConstantPropagationProblemExtended
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaIDEAnalysis

/**
 * Extended linear constant propagation as IDE analysis
 */
class LinearConstantPropagationAnalysisExtended(project: SomeProject)
    extends JavaIDEAnalysis(
        project,
        new LinearConstantPropagationProblemExtended(project),
        LinearConstantPropagationPropertyMetaInformation
    ) {
    val lcpProblemExtended: LinearConstantPropagationProblemExtended =
        problem.asInstanceOf[LinearConstantPropagationProblemExtended]

    val lcpProblem: LinearConstantPropagationProblem =
        lcpProblemExtended
}
