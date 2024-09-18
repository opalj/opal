/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEProblem
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationProblem
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Linear constant propagation as IDE analysis
 */
abstract class LinearConstantPropagationAnalysisScheduler
    extends JavaIDEAnalysisScheduler[LinearConstantPropagationFact, LinearConstantPropagationValue] {
    override def propertyMetaInformation: IDEPropertyMetaInformation[
        LinearConstantPropagationFact,
        LinearConstantPropagationValue
    ] = LinearConstantPropagationPropertyMetaInformation

    override def createProblem(project: SomeProject): IDEProblem[
        LinearConstantPropagationFact,
        LinearConstantPropagationValue,
        JavaStatement,
        Method
    ] = {
        new LinearConstantPropagationProblem(project)
    }
}
