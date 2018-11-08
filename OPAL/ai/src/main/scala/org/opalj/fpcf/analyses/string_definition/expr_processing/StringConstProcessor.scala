/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.properties.StringConstancyLevel
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst

import scala.collection.mutable.ArrayBuffer

/**
 * This implementation of [[AbstractExprProcessor]] processes [[org.opalj.tac.StringConst]]
 * expressions.
 *
 * @author Patrick Mell
 */
class StringConstProcessor() extends AbstractExprProcessor {

    /**
     * For this implementation, `stmts` is not needed (thus, you may pass an empty Array). `expr` is
     * required to be of type [[org.opalj.tac.StringConst]] (otherwise `None` will be returned).
     *
     * @see [[AbstractExprProcessor.process]]
     */
    override def process(stmts: Array[Stmt[V]], expr: Expr[V]): Option[StringConstancyProperty] =
        expr match {
            case strConst: StringConst ⇒ Some(StringConstancyProperty(
                StringConstancyLevel.CONSTANT, ArrayBuffer(strConst.value)
            ))
            case _ ⇒ None
        }

}
