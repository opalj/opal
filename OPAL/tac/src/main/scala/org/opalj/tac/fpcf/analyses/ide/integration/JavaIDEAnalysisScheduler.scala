/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.integration

import org.opalj.br.Method
import org.opalj.ide.integration.IDEAnalysisScheduler
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized IDE analysis scheduler for Java programs
 */
abstract class JavaIDEAnalysisScheduler[Fact <: IDEFact, Value <: IDEValue]
    extends IDEAnalysisScheduler[Fact, Value, JavaStatement, Method]
