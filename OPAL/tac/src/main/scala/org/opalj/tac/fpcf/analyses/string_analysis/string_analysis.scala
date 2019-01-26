/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.tac.DUVar

/**
 * @author Patrick Mell
 */
package object string_analysis {

    /**
     * The type of entities the [[LocalStringAnalysis]] processes.
     *
     * @note The analysis requires further context information, see [[P]].
     */
    type V = DUVar[ValueInformation]

    /**
     * [[LocalStringAnalysis]] processes a local variable within the context of a
     * particular context, i.e., the method in which it is used.
     */
    type P = (V, Method)

}
