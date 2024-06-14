/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation

import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaIDEAnalysis

/**
 * Linear constant propagation as IDE analysis
 */
class LinearConstantPropagationAnalysis(project: SomeProject)
    extends JavaIDEAnalysis(
        project,
        new LinearConstantPropagationProblem(project),
        LinearConstantPropagationPropertyMetaInformation
    ) {
    val lcpProblem: LinearConstantPropagationProblem =
        problem.asInstanceOf[LinearConstantPropagationProblem]
}
