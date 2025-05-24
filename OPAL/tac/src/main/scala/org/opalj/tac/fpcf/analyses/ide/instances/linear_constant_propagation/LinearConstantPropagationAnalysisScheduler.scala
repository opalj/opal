/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package linear_constant_propagation

import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationProblem
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisSchedulerBase
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.problem.JavaIDEProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG

/**
 * Linear constant propagation as IDE analysis.
 *
 * @author Robin Körkemeier
 */
class LinearConstantPropagationAnalysisScheduler
    extends JavaIDEAnalysisScheduler[LinearConstantPropagationFact, LinearConstantPropagationValue]
    with JavaIDEAnalysisSchedulerBase.ForwardICFG {
    override def propertyMetaInformation: JavaIDEPropertyMetaInformation[
        LinearConstantPropagationFact,
        LinearConstantPropagationValue
    ] = LinearConstantPropagationPropertyMetaInformation

    override def createProblem(project: SomeProject, icfg: JavaICFG): JavaIDEProblem[
        LinearConstantPropagationFact,
        LinearConstantPropagationValue
    ] = {
        new LinearConstantPropagationProblem
    }
}
