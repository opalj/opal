/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing

import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.EmptyIntTrieSet
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
import org.opalj.tac.TACStmts

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
            case _: New ⇒
                val useSites = assignment.targetVar.usedBy.filter(!ignore.contains(_))
                val (inits, nonInits) = getInitsAndNonInits(useSites, stmts)
                val initTreeNodes = ListBuffer[StringTree]()
                val nonInitTreeNodes = ListBuffer[StringTree]()

                inits.foreach { next ⇒
                    val toProcess = stmts(next) match {
                        case init: NonVirtualMethodCall[V] if init.params.nonEmpty ⇒
                            init.params.head.asVar.definedBy
                        case assignment: Assignment[V] ⇒
                            assignment.expr.asVirtualFunctionCall.receiver.asVar.definedBy
                        case _ ⇒
                            EmptyIntTrieSet
                    }
                    exprHandler.processDefSites(toProcess) match {
                        case Some(toAppend) ⇒ initTreeNodes.append(toAppend)
                        case None           ⇒
                    }
                }
                // No argument to constructor was passed => empty string with nonInits as child
                if (initTreeNodes.isEmpty) {
                    initTreeNodes.append(TreeValueElement(
                        None, StringConstancyInformation(CONSTANT, "")
                    ))
                }

                nonInits.foreach { next ⇒
                    val tree = exprHandler.processDefSite(next)
                    if (tree.isDefined) {
                        nonInitTreeNodes.append(tree.get)
                    }
                }

                if (nonInitTreeNodes.nonEmpty) {
                    initTreeNodes.foreach { next ⇒
                        val toAppend = nonInitTreeNodes.size match {
                            case 1 ⇒ nonInitTreeNodes.head
                            case _ ⇒ TreeConditionalElement(nonInitTreeNodes)
                        }
                        next match {
                            case tve: TreeValueElement ⇒ tve.child = Some(toAppend)
                            case _                     ⇒ next.children.append(toAppend)
                        }
                    }
                }

                initTreeNodes.size match {
                    case 1 ⇒ Some(initTreeNodes.head)
                    case _ ⇒ Some(TreeConditionalElement(initTreeNodes))
                }
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
                // Constructors are identified by the "init" method and assignments (ExprStmts, in
                // contrast, point to non-constructor related calls)
                case mc: NonVirtualMethodCall[V] if mc.name == "<init>" ⇒ inits.append(next)
                case _: Assignment[V]                                   ⇒ inits.append(next)
                case _                                                  ⇒ nonInits.append(next)
            }
        }
        (inits.toList, nonInits.toList)
    }

}
