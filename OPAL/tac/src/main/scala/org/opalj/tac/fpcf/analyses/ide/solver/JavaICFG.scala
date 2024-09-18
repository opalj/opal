/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.solver

import org.opalj.br.Method
import org.opalj.ide.solver.ICFG

/**
 * Interprocedural control flow graph for Java programs
 */
trait JavaICFG extends ICFG[JavaStatement, Method] {
    def getCallablesCallableFromOutside: collection.Set[Method]
}
