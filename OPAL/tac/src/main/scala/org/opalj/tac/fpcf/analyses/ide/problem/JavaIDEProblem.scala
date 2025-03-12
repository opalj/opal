/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package problem

import org.opalj.br.Method
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized IDE problem for Java programs
 */
abstract class JavaIDEProblem[Fact <: IDEFact, Value <: IDEValue] extends IDEProblem[Fact, Value, JavaStatement, Method]
