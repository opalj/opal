/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.ifds.problem

import org.opalj.br.analyses.SomeProject
import org.opalj.ide.problem.IDEFact
import org.opalj.tac.fpcf.analyses.ide.solver.JavaBackwardICFG

/**
 * Specialized IFDS problem for Java programs on a backward ICFG based on an IDE problem
 */
abstract class JavaBackwardIFDSProblem[Fact <: IDEFact](
    override val icfg: JavaBackwardICFG
) extends JavaIFDSProblem[Fact](icfg) {
    def this(project: SomeProject) = {
        this(new JavaBackwardICFG(project))
    }
}
