/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.fpcf.string_definition.properties.StringConstancyLevel.CONSTANT
import org.opalj.tac.Stmt
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.TreeConditionalElement
import org.opalj.fpcf.string_definition.properties.TreeValueElement
import org.opalj.tac.Assignment
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
     * @see [[AbstractExprProcessor.process()]]
     */
    override def process(
        assignment: Assignment[V], stmts: Array[Stmt[V]], ignore: List[Int] = List[Int]()
    ): Option[StringTree] = {
        assignment.expr match {
            case _: New ⇒
                val inits = assignment.targetVar.usedBy.filter {
                    stmts(_) match {
                        case mc: NonVirtualMethodCall[V] if mc.name == "<init>" ⇒ true
                        case _                                                  ⇒ false
                    }
                }
                val treeNodes = ListBuffer[Option[StringTree]]()

                inits.foreach { next ⇒
                    if (!ignore.contains(next)) {
                        val init = stmts(next).asNonVirtualMethodCall
                        if (init.params.nonEmpty) {
                            treeNodes.append(
                                exprHandler.processDefSites(init.params.head.asVar.definedBy)
                            )
                        }
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

}
