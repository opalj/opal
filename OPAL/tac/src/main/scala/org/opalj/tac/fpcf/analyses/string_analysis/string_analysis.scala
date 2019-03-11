/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.value.ValueInformation
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.StringConstancyProperty
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

    /**
     * This type indicates how (non-final) parameters of functions are represented. The outer-most
     * list holds all parameter lists a function is called with. The list in the middle holds the
     * parameters of a concrete call and the inner-most list holds interpreted parameters. The
     * reason for the inner-most list is that a parameter might have different definition sites; to
     * capture all, the third (inner-most) list is necessary.
     */
    type NonFinalFunctionArgs = ListBuffer[ListBuffer[ListBuffer[EOptionP[Entity, StringConstancyProperty]]]]

    /**
     * This type serves as a lookup mechanism to find out which functions parameters map to which
     * argument position. That is, the element of type [[P]] of the inner map maps from this entity
     * to its position in a data structure of type [[NonFinalFunctionArgs]]. The outer map is
     * necessary to uniquely identify a position as an entity might be used for different function
     * calls.
     */
    type NonFinalFunctionArgsPos = mutable.Map[FunctionCall[V], mutable.Map[P, (Int, Int, Int)]]

}
