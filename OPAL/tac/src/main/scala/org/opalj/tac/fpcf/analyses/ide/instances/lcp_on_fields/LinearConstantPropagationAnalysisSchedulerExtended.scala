/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields

import scala.collection.immutable

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.PropertyBounds
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEProblem
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LinearConstantPropagationProblemExtended
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.LinearConstantPropagationPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEAnalysisScheduler
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Extended linear constant propagation as IDE analysis
 */
abstract class LinearConstantPropagationAnalysisSchedulerExtended
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
        new LinearConstantPropagationProblemExtended(project)
    }

    override def uses: Set[PropertyBounds] =
        super.uses.union(immutable.Set(
            PropertyBounds.ub(LCPOnFieldsPropertyMetaInformation)
        ))
}
