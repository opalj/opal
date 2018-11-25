/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation

import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.tac.Stmt
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.New
import org.opalj.tac.TACStmts
import org.opalj.tac.VirtualFunctionCall

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 *
 * @author Patrick Mell
 */
class NewStringBuilderProcessor(
        private val exprHandler: ExprHandler
) extends AbstractExprProcessor {

    /**
     * `expr` of `assignment`is required to be of type [[org.opalj.tac.New]] (otherwise `None` will
     * be returned).
     *
     * @see [[AbstractExprProcessor.processAssignment]]
     */
    override def processAssignment(
        assignment: Assignment[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = {
        assignment.expr match {
            case _ ⇒ None
        }
    }

    /**
     * This implementation does not change / implement the behavior of
     * [[AbstractExprProcessor.processExpr]].
     */
    override def processExpr(
        expr: Expr[V], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]],
        ignore: List[Int] = List[Int]()
    ): Option[StringTree] = super.processExpr(expr, stmts, cfg, ignore)

}

object NewStringBuilderProcessor {

    /**
     * Determines the definition site of the initialization of the base object that belongs to a
     * ''toString'' call.
     *
     * @param toString The ''toString'' call of the object for which to get the initialization def
     *                 site for. Make sure that the object is a subclass of
     *                 [[AbstractStringBuilder]].
     * @param stmts A list of statements which will be used to lookup which one the initialization
     *              is.
     * @return Returns the definition site of the base object of the call. If something goes wrong,
     *         e.g., no initialization is found, ''None'' is returned.
     */
    def findDefSiteOfInit(toString: VirtualFunctionCall[V], stmts: Array[Stmt[V]]): List[Int] = {
        // TODO: Check that we deal with an instance of AbstractStringBuilder
        if (toString.name != "toString") {
            return List()
        }

        val defSites = ListBuffer[Int]()
        val stack = mutable.Stack[Int](toString.receiver.asVar.definedBy.toArray: _*)
        while (stack.nonEmpty) {
            val next = stack.pop()
            stmts(next) match {
                case a: Assignment[V] ⇒
                    a.expr match {
                        case _: New ⇒
                            defSites.append(next)
                        case vfc: VirtualFunctionCall[V] ⇒
                            stack.pushAll(vfc.receiver.asVar.definedBy.toArray)
                    }
                case _ ⇒
            }
        }

        defSites.sorted.toList
    }

}
