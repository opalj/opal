/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.problem

import org.opalj.br.Method
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized IDE problem for Java programs
 */
abstract class JavaIDEProblem[Fact <: IDEFact, Value <: IDEValue] extends IDEProblem[Fact, Value, JavaStatement, Method]
