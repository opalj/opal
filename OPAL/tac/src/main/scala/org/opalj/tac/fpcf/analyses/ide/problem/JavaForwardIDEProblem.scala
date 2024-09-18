/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.problem

import org.opalj.br.analyses.SomeProject
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.solver.JavaForwardICFG

/**
 * Specialized IDE problem for Java programs on a forward ICFG
 */
abstract class JavaForwardIDEProblem[Fact <: IDEFact, Value <: IDEValue](
    override val icfg: JavaForwardICFG
) extends JavaIDEProblem[Fact, Value](icfg) {
    def this(project: SomeProject) = {
        this(new JavaForwardICFG(project))
    }
}
