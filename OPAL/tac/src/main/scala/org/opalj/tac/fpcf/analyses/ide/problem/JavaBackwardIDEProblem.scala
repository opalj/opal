/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.problem

import org.opalj.br.analyses.SomeProject
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.solver.JavaBackwardICFG

/**
 * Specialized IDE problem for Java programs on a backward ICFG
 */
abstract class JavaBackwardIDEProblem[Fact <: IDEFact, Value <: IDEValue](
    override val icfg: JavaBackwardICFG
) extends JavaIDEProblem[Fact, Value](icfg) {
    def this(project: SomeProject) = {
        this(new JavaBackwardICFG(project))
    }
}
