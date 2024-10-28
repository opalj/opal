/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.integration

import org.opalj.br.analyses.SomeProject
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.problem.JavaIDEProblem
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG

/**
 * Specialized IDE analysis scheduler for Java programs
 */
abstract class JavaIDEAnalysisScheduler[Fact <: IDEFact, Value <: IDEValue]
    extends JavaIDEAnalysisSchedulerBase[Fact, Value] {
    override def createProblem(project: SomeProject, icfg: JavaICFG): JavaIDEProblem[Fact, Value]
}
