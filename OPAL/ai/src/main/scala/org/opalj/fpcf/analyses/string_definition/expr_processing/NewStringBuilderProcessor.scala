/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.CONSTANT
import org.opalj.tac.Stmt
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.TreeConditionalElement
import org.opalj.fpcf.string_definition.properties.TreeValueElement
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.New
import org.opalj.tac.NonVirtualMethodCall

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
     * @see [[AbstractExprProcessor.processAssignment()]]
     */
    override def processAssignment(
        assignment: Assignment[V], stmts: Array[Stmt[V]], ignore: List[Int] = List[Int]()
    ): Option[StringTree] = {
        assignment.expr match {
            case _: New ⇒
                val useSites = assignment.targetVar.usedBy.filter(!ignore.contains(_))
                val (inits, nonInits) = getInitsAndNonInits(useSites, stmts)
                val treeNodes = ListBuffer[Option[StringTree]]()

                inits.foreach { next ⇒
                    val init = stmts(next).asNonVirtualMethodCall
                    if (init.params.nonEmpty) {
                        treeNodes.append(
                            exprHandler.processDefSites(init.params.head.asVar.definedBy)
                        )
                    }
                }

                nonInits.foreach { next ⇒
                    val tree = exprHandler.processDefSite(next)
                    if (tree.isDefined) {
                        treeNodes.append(tree)
                    }
                }

                treeNodes.size match {
                    case 0 ⇒
                        // No argument to constructor was passed => empty string
                        Some(TreeValueElement(None, StringConstancyInformation(CONSTANT, "")))
                    case 1 ⇒ treeNodes.head
                    case _ ⇒ Some(TreeConditionalElement(
                        treeNodes.filter(_.isDefined).map(_.get)
                    ))
                }
            case _ ⇒ None
        }
    }

    /**
     * This implementation does not change / implement the behavior of
     * [[AbstractExprProcessor.processExpr]].
     */
    override def processExpr(
        expr: Expr[V], stmts: Array[Stmt[V]], ignore: List[Int]
    ): Option[StringTree] = super.processExpr(expr, stmts, ignore)

    /**
     *
     * @param useSites Not-supposed to contain already processed sites.
     * @param stmts    A list of statements (the one that was passed on to the `process`function of
     *                 this class).
     * @return
     */
    private def getInitsAndNonInits(
        useSites: IntTrieSet, stmts: Array[Stmt[V]]
    ): (List[Int], List[Int]) = {
        val inits = ListBuffer[Int]()
        val nonInits = ListBuffer[Int]()
        useSites.foreach { next ⇒
            stmts(next) match {
                case mc: NonVirtualMethodCall[V] if mc.name == "<init>" ⇒
                    inits.append(next)
                case _ ⇒
                    nonInits.append(next)
            }
        }
        (inits.toList, nonInits.toList)
    }

}
