/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.integration

import org.opalj.br.Method
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.ide.problem.IDEFact
import org.opalj.ide.problem.IDEValue
import org.opalj.tac.fpcf.analyses.ide.solver.JavaStatement

/**
 * Specialized property meta information for IDE problems with Java programs
 */
trait JavaIDEPropertyMetaInformation[Fact <: IDEFact, Value <: IDEValue]
    extends IDEPropertyMetaInformation[Fact, Value, JavaStatement, Method]
