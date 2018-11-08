/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing

import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.tac.Stmt
import org.opalj.fpcf.string_definition.properties.StringTree
import org.opalj.fpcf.string_definition.properties.StringTreeConcat
import org.opalj.fpcf.string_definition.properties.StringTreeConst
import org.opalj.fpcf.string_definition.properties.StringTreeOr
import org.opalj.fpcf.string_definition.properties.StringTreeRepetition
import org.opalj.graphs.DominatorTree
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.New
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.TACStmts

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
            case _: New ⇒
                val uses = assignment.targetVar.usedBy.filter(!ignore.contains(_)).toArray.sorted
                val (inits, nonInits) = getInitsAndNonInits(uses, stmts, cfg)
                val initTreeNodes = ListBuffer[StringTree]()
                val nonInitTreeNodes = ListBuffer[StringTree]()

                inits.foreach { next ⇒
                    val toProcess = stmts(next) match {
                        case init: NonVirtualMethodCall[V] if init.params.nonEmpty ⇒
                            init.params.head.asVar.definedBy.toArray.sorted
                        case assignment: Assignment[V] ⇒
                            val vfc = assignment.expr.asVirtualFunctionCall
                            var defs = vfc.receiver.asVar.definedBy
                            if (vfc.params.nonEmpty) {
                                vfc.params.head.asVar.definedBy.foreach(defs += _)
                            }
                            defs ++= assignment.targetVar.asVar.usedBy
                            defs.toArray.sorted
                        case _ ⇒
                            Array()
                    }
                    val processed = if (toProcess.length == 1) {
                        val intermRes = exprHandler.processDefSite(toProcess.head)
                        if (intermRes.isDefined) intermRes else None
                    } else {
                        val children = toProcess.map(exprHandler.processDefSite).
                            filter(_.isDefined).map(_.get)
                        children.length match {
                            case 0 ⇒ None
                            case 1 ⇒ Some(children.head)
                            case _ ⇒ Some(StringTreeConcat(children.to[ListBuffer]))
                        }
                    }
                    if (processed.isDefined) {
                        initTreeNodes.append(processed.get)
                    }
                }

                nonInits.foreach { next ⇒
                    val subtree = exprHandler.concatDefSites(next)
                    if (subtree.isDefined) {
                        nonInitTreeNodes.append(subtree.get)
                    }
                }

                if (initTreeNodes.isEmpty && nonInitTreeNodes.isEmpty) {
                    return None
                }

                // Append nonInitTreeNodes to initTreeNodes (as children)
                if (nonInitTreeNodes.nonEmpty) {
                    val toAppend = nonInitTreeNodes.size match {
                        case 1 ⇒ nonInitTreeNodes.head
                        case _ ⇒ StringTreeOr(nonInitTreeNodes)
                    }
                    if (initTreeNodes.isEmpty) {
                        initTreeNodes.append(toAppend)
                    } else {
                        initTreeNodes.zipWithIndex.foreach {
                            case (rep: StringTreeRepetition, _) ⇒ rep.child = toAppend
                            // We cannot add to a constant element => slightly rearrange the tree
                            case (const: StringTreeConst, index) ⇒
                                initTreeNodes(index) = StringTreeConcat(ListBuffer(const, toAppend))
                            case (next, _) ⇒ next.children.append(toAppend)
                        }
                    }
                }

                initTreeNodes.size match {
                    case 1 ⇒ Some(initTreeNodes.head)
                    case _ ⇒ Some(StringTreeOr(initTreeNodes))
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
     * @param useSites Not-supposed to contain already processed sites. Also, they should be in
     *                 ascending order.
     * @param stmts    A list of statements (the one that was passed on to the `process`function of
     *                 this class).
     * @return
     */
    private def getInitsAndNonInits(
        useSites: Array[Int], stmts: Array[Stmt[V]], cfg: CFG[Stmt[V], TACStmts[V]]
    ): (List[Int], List[List[Int]]) = {
        val domTree = cfg.dominatorTree
        val inits = ListBuffer[Int]()
        var nonInits = ListBuffer[Int]()

        useSites.foreach { next ⇒
            stmts(next) match {
                // Constructors are identified by the "init" method and assignments (ExprStmts, in
                // contrast, point to non-constructor related calls)
                case mc: NonVirtualMethodCall[V] if mc.name == "<init>" ⇒ inits.append(next)
                case _: Assignment[V] ⇒
                    // Use dominator tree to determine whether init or noninit
                    if (doesDominate(inits.toArray, next, domTree)) {
                        nonInits.append(next)
                    } else {
                        inits.append(next)
                    }
                case _ ⇒ nonInits.append(next)
            }
        }
        // Sort in descending order to enable correct grouping in the next step
        nonInits = nonInits.sorted.reverse

        // Next, group all non inits into lists depending on their basic block in the CFG; as the
        // "belongs to parent" relationship is transitive, there are two approaches: 1) recursively
        // check or 2) store grandchildren in a flat structure as well. Here, 2) is implemented as
        // only references are stored which is not so expensive. However, this leads to the fact
        // that we need to create a distinct map before returning (see declaration of uniqueLists
        // below)
        val blocks = mutable.LinkedHashMap[BasicBlock, ListBuffer[Int]]()
        nonInits.foreach { next ⇒
            val nextBlock = cfg.bb(next)
            val parentBlock = nextBlock.successors.filter {
                case bb: BasicBlock ⇒ blocks.contains(bb)
                case _              ⇒ false
            }
            if (parentBlock.nonEmpty) {
                val list = blocks(parentBlock.head.asBasicBlock)
                list.append(next)
                blocks += (nextBlock → list)
            } else {
                blocks += (nextBlock → ListBuffer[Int](next))
            }
        }

        // Make the list unique (as described above) and sort it in ascending order
        val uniqueLists = blocks.values.toList.distinct.reverse
        (inits.toList.sorted, uniqueLists.map(_.toList))
    }

    /**
     * `doesDominate` checks if a list of `possibleDominators` dominates another statement,
     * `toCheck`, by using the given dominator tree, `domTree`. If so, true is returned and false
     * otherwise.
     */
    private def doesDominate(
        possibleDominators: Array[Int], toCheck: Int, domTree: DominatorTree
    ): Boolean = {
        var nextToCheck = toCheck
        var pd = possibleDominators.filter(_ < nextToCheck)

        while (pd.nonEmpty) {
            if (possibleDominators.contains(nextToCheck)) {
                return true
            }

            nextToCheck = domTree.dom(nextToCheck)
            pd = pd.filter(_ <= nextToCheck)
        }

        false
    }

}
