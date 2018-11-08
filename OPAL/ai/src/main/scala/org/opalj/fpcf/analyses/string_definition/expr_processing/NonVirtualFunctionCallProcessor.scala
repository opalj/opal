/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing

import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.DYNAMIC
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.TreeValueElement
import org.opalj.tac.Assignment
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt

/**
 * This implementation of [[AbstractExprProcessor]] processes
 * [[org.opalj.tac.NonVirtualFunctionCall]] expressions.
 * Currently, this implementation is only a rough approximation in the sense that all
 * `NonVirtualFunctionCall`s are processed by returning a [[TreeValueElement]] with no children
 * and `StringConstancyProperty(DYNAMIC, ArrayBuffer("*"))` as a value (i.e., it does not analyze
 * the function call in depth).
 *
 * @author Patrick Mell
 */
class NonVirtualFunctionCallProcessor() extends AbstractExprProcessor {

    /**
     * `expr` of `assignment`is required to be of type [[org.opalj.tac.NonVirtualFunctionCall]]
     * (otherwise `None` will be returned).
     * `stmts` currently is not relevant, thus an empty array may be passed.
     *
     * @see [[AbstractExprProcessor#process]]
     */
    override def process(assignment: Assignment[V], stmts: Array[Stmt[V]]): Option[StringTree] =
        assignment.expr match {
            case _: NonVirtualFunctionCall[V] ⇒ Some(TreeValueElement(
                None, StringConstancyInformation(DYNAMIC, "*")
            ))
            case _ ⇒ None
        }

}