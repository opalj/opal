/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.tac.DUVar
import org.opalj.tac.FunctionCall

/**
 * @author Patrick Mell
 */
package object string_analysis {

    /**
     * The type of entities the [[IntraproceduralStringAnalysis]] processes.
     *
     * @note The analysis requires further context information, see [[P]].
     */
    type V = DUVar[ValueInformation]

    /**
     * [[IntraproceduralStringAnalysis]] processes a local variable within the context of a
     * particular context, i.e., the method in which it is used.
     */
    type P = (V, Method)

    type NonFinalFunctionArgsPos = mutable.Map[FunctionCall[V], mutable.Map[P, (Int, Int, Int)]]
    type NonFinalFunctionArgs = ListBuffer[ListBuffer[ListBuffer[ProperPropertyComputationResult]]]

}
