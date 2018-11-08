/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses

import org.opalj.br.Method
import org.opalj.tac.DUVar
import org.opalj.value.ValueInformation

/**
 * @author Patrick Mell
 */
package object string_definition {

    /**
     * The type of entities the [[LocalStringDefinitionAnalysis]] processes.
     *
     * @note The analysis requires further context information, see [[P]].
     */
    type V = DUVar[ValueInformation]

    /**
     * [[LocalStringDefinitionAnalysis]] processes a local variable within the context of a
     * particular context, i.e., the method in which it is declared and used.
     */
    type P = (V, Method)

}
