/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.ifds.problem

import org.opalj.br.Method
import org.opalj.ide.ifds.problem.IFDSProblem
import org.opalj.ide.problem.IDEFact
import org.opalj.tac.fpcf.analyses.ide.solver.JavaICFG
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized IFDS problem for Java programs based on an IDE problem
 */
abstract class JavaIFDSProblem[Fact <: IDEFact](
    override val icfg: JavaICFG
) extends IFDSProblem[Fact, JavaStatement, Method](icfg)
