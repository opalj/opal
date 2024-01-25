/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.Method
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP

/**
 * @author Patrick Mell
 */
package object string_analysis {

    /**
     * The type of entities the [[IntraproceduralStringAnalysis]] and the [[InterproceduralStringAnalysis]] process.
     *
     * @note The analysis require further context information, see [[SContext]].
     */
    type SEntity = PV

    /**
     * [[IntraproceduralStringAnalysis]] and [[InterproceduralStringAnalysis]] process a local variable within a
     * particular context, i.e. the method in which it is used.
     */
    type SContext = (SEntity, Method)

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
     * argument position. That is, the element of type [[SContext]] of the inner map maps from this entity
     * to its position in a data structure of type [[NonFinalFunctionArgs]]. The outer map is
     * necessary to uniquely identify a position as an entity might be used for different function
     * calls.
     */
    type NonFinalFunctionArgsPos = mutable.Map[FunctionCall[V], mutable.Map[SContext, (Int, Int, Int)]]

}
