/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing
import org.opalj.tac.Expr
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.properties.StringConstancyLevel.DYNAMIC
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt

import scala.collection.mutable.ArrayBuffer

/**
 * This implementation of [[AbstractExprProcessor]] processes
 * [[org.opalj.tac.NonVirtualFunctionCall]] expressions.
 * Currently, this implementation is only a rough approximation in the sense that all
 * `NonVirtualFunctionCall`s are processed by returning
 * `StringConstancyProperty(DYNAMIC, ArrayBuffer("*"))`, i.e., do not analyze the function call in
 * depth.
 *
 * @author Patrick Mell
 */
class NonVirtualFunctionCallProcessor() extends AbstractExprProcessor {

    /**
     * `expr` is required to be of type [[org.opalj.tac.NonVirtualFunctionCall]] (otherwise `None`
     * will be returned).
     * `stmts` currently is not relevant, thus an empty array may be passed.
     *
     * @see [[AbstractExprProcessor#process]]
     */
    override def process(stmts: Array[Stmt[V]], expr: Expr[V]): Option[StringConstancyProperty] =
        expr match {
            case _: NonVirtualFunctionCall[V] ⇒
                Some(StringConstancyProperty(DYNAMIC, ArrayBuffer("*")))
            case _ ⇒ None
        }

}