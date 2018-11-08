/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing
import org.opalj.tac.Expr
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Stmt

import scala.collection.mutable.ArrayBuffer

/**
 * This implementation of [[AbstractExprProcessor]] processes [[org.opalj.tac.ArrayLoad]]
 * expressions.
 *
 * @param exprHandler As this expression processor will encounter other expressions outside its
 *                    scope, such as StringConst or NonVirtualFunctionCall, an [[ExprHandler]] is
 *                    required.
 *
 * @author Patrick Mell
 */
class ArrayLoadProcessor(
        private val exprHandler: ExprHandler
) extends AbstractExprProcessor {

    /**
     * `expr` is required to be of type [[org.opalj.tac.ArrayLoad]] (otherwise `None` will be
     * returned).
     *
     * @see [[AbstractExprProcessor.process]]
     */
    override def process(stmts: Array[Stmt[V]], expr: Expr[V]): Option[StringConstancyProperty] = {
        expr match {
            case al: ArrayLoad[V] ⇒
                val properties = ArrayBuffer[StringConstancyProperty]()
                al.arrayRef.asVar.definedBy.foreach { defSite ⇒
                    val arrDecl = stmts(defSite)
                    arrDecl.asAssignment.targetVar.usedBy.filter {
                        stmts(_).isInstanceOf[ArrayStore[V]]
                    } foreach { f: Int ⇒
                        properties.appendAll(exprHandler.processDefinitionSites(
                            stmts(f).asArrayStore.value.asVar.definedBy
                        ))
                    }
                }

                StringConstancyProperty.reduce(properties.toArray)
            case _ ⇒ None
        }
    }

}
