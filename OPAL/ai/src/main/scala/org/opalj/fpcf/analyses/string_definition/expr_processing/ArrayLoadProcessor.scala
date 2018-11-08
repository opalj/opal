/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing
import org.opalj.tac.Expr
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.properties.StringConstancyLevel
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Stmt

import scala.collection.mutable.ArrayBuffer

/**
 * This implementation of [[AbstractExprProcessor]] processes [[org.opalj.tac.ArrayLoad]]
 * expressions.
 *
 * @author Patrick Mell
 */
class ArrayLoadProcessor() extends AbstractExprProcessor {

    /**
     * `expr` is required to be of type [[org.opalj.tac.ArrayLoad]] (otherwise `None` will be
     * returned).
     *
     * @see [[AbstractExprProcessor#process]]
     */
    override def process(stmts: Array[Stmt[V]], expr: Expr[V]): Option[StringConstancyProperty] = {
        expr match {
            case al: ArrayLoad[V] ⇒
                val arrRef = al.arrayRef
                val arrDecl = stmts(arrRef.asVar.definedBy.head)
                val arrValues = arrDecl.asAssignment.targetVar.usedBy.filter {
                    stmts(_).isInstanceOf[ArrayStore[V]]
                } map { f: Int ⇒
                    val defSite = stmts(f).asArrayStore.value.asVar.definedBy.head
                    stmts(defSite).asAssignment.expr.asStringConst.value
                }

                Some(StringConstancyProperty(
                    StringConstancyLevel.CONSTANT, arrValues.to[ArrayBuffer]
                ))
            case _ ⇒ None
        }
    }

}
