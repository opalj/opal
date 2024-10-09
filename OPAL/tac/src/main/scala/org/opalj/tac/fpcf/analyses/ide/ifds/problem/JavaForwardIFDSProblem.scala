/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.ifds.problem

import org.opalj.br.analyses.SomeProject
import org.opalj.ide.problem.IDEFact
import org.opalj.tac.fpcf.analyses.ide.solver.JavaForwardICFG

/**
 * Specialized IFDS problem for Java programs on a forward ICFG based on an IDE problem
 */
abstract class JavaForwardIFDSProblem[Fact <: IDEFact](
    override val icfg: JavaForwardICFG
) extends JavaIFDSProblem[Fact](icfg) {
    def this(project: SomeProject) = {
        this(new JavaForwardICFG(project))
    }
}
