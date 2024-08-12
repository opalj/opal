/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.solver

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEProblem
import org.opalj.ide.problem.IDEValue
import org.opalj.ide.solver.IDEAnalysis

/**
 * Solver for IDE problems specialized for Java programs
 */
class JavaIDEAnalysis[Fact <: IDEFact, Value <: IDEValue](
    project:                 SomeProject,
    problem:                 IDEProblem[Fact, Value, JavaStatement, Method],
    propertyMetaInformation: IDEPropertyMetaInformation[Fact, Value]
) extends IDEAnalysis[Fact, Value, JavaStatement, Method](project, problem, propertyMetaInformation)
