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
                val nonInitTreeNodes = mutable.Map[Int, ListBuffer[StringTree]]()

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
                        val key = next.min
                        if (nonInitTreeNodes.contains(key)) {
                            nonInitTreeNodes(key).append(subtree.get)
                        } else {
                            nonInitTreeNodes(key) = ListBuffer(subtree.get)
                        }
                    }
                }

                if (initTreeNodes.isEmpty && nonInitTreeNodes.isEmpty) {
                    return None
                }

                // Append nonInitTreeNodes to initTreeNodes (as children)
                if (nonInitTreeNodes.nonEmpty) {
                    val toAppend = nonInitTreeNodes.size match {
                        // If there is only one element in the map use this
                        case 1 ⇒ nonInitTreeNodes.head._2.head
                        // Otherwise, we need to build the proper tree, considering dominators
                        case _ ⇒ orderNonInitNodes(nonInitTreeNodes, cfg)
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
                    if (domTree.doesDominate(inits.toArray, next)) {
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
     * The relation of nodes is not obvious, i.e., one cannot generally say that two nodes, which,
     * e.g., modify a [[StringBuilder]] object are concatenations or  are to be mapped to a
     * [[StringTreeOr]].
     * <p>
     * This function uses the following algorithm to determine the relation of the given nodes:
     * <ol>
     *   <li>
     *       For each given node, compute the dominators (and store it in lists called
     *       ''dominatorLists'').</li>
     *   <li>
     *       Take all ''dominatorLists'' and union them (without duplicates) and sort this list in
     *       descending order. This list, called ''uniqueDomList'', is then used in the next step.
     *   </li>
     *   <li>
     *       Traverse ''uniqueDomList''. Here, we call the next element in that list ''nextDom''.
     *       One of the following cases will occur:
     *       <ol>
     *           <li>
     *               Only one [[StringTree]] element in ''treeNodes'' has ''nextDom'' as a
     *               dominator. In this case, nothing is done as we cannot say anything about the
     *               relation to other elements in ''treeNodes''.
     *           </li>
     *           <li>
     *               At least two elements in ''treeNodes'' have ''nextDom'' as a dominator. In this
     *               case, these nodes are in a 'only-one-node-is-evaluated-relation' as it happens
     *               when the dominator of two nodes is an if condition, for example. Thus, these
     *               elements are put into a [[StringTreeOr]] element and then no longer considered
     *               for the computation.
     *           </li>
     *       </ol>
     *   </li>
     *   <li>
     *       It might be that not all elements in ''treeNodes'' were processed by the second case of
     *       the previous step (normally, this should be only one element. These elements represent
     *       a concatenation relation. Thus, all [[StringTreeOr]] elements of the last step are then
     *       put into a [[StringTreeConcat]] element with the ones not processed yet. They are
     *       ordered in ascending order according to their index in the statement list to preserve
     *       the correct order.
     *   </li>
     * </ol>
     *
     * @param treeNodes The nodes which are to be ordered. The key of the map refers to the index in
     *                  the statement list. It might be that some operation (e.g., append) have two
     *                  definition sites; In this case, pass the minimum index. The values of the
     *                  map correspond to [[StringTree]] elements that resulted from the evaluation
     *                  of the definition site(s).
     * @param cfg The control flow graph.
     * @return This function computes the correct relation of the given [[StringTree]] elements
     *         and returns this as a single tree element.
     */
    private def orderNonInitNodes(
        treeNodes: mutable.Map[Int, ListBuffer[StringTree]],
        cfg:       CFG[Stmt[V], TACStmts[V]]
    ): StringTree = {
        // TODO: Put this function in a different place (like DominatorTreeUtils) and generalize the procedure.
        val domTree = cfg.dominatorTree
        // For each list of nodes, get the dominators
        val rootIndex = cfg.startBlock.startPC
        val dominatorLists = mutable.Map[Int, List[Int]]()
        for ((key, _) ← treeNodes) {
            val dominators = ListBuffer[Int]()
            var next = domTree.immediateDominators(key)
            while (next != rootIndex) {
                dominators.prepend(next)
                next = domTree.immediateDominators(next)
            }
            dominators.prepend(rootIndex)
            dominatorLists(key) = dominators.toList
        }

        // Build a unique union of the dominators that is in descending order
        var uniqueDomList = ListBuffer[Int]()
        for ((_, value) ← dominatorLists) {
            uniqueDomList.append(value: _*)
        }
        uniqueDomList = uniqueDomList.distinct.sorted.reverse

        val newChildren = ListBuffer[StringTree]()
        uniqueDomList.foreach { nextDom ⇒
            // Find elements with the same dominator
            val indicesWithSameDom = ListBuffer[Int]()
            for ((key, value) ← dominatorLists) {
                if (value.contains(nextDom)) {
                    indicesWithSameDom.append(key)
                }
            }
            if (indicesWithSameDom.size > 1) {
                val newOrElement = ListBuffer[StringTree]()
                // Sort in order to have the correct order
                indicesWithSameDom.sorted.foreach { nextIndex ⇒
                    newOrElement.append(treeNodes(nextIndex).head)
                    dominatorLists.remove(nextIndex)
                }
                newChildren.append(StringTreeOr(newOrElement))
            }
        }

        // If there are elements left, add them as well (they represent concatenations)
        for ((key, _) ← dominatorLists) { newChildren.append(treeNodes(key).head) }

        if (newChildren.size == 1) {
            newChildren.head
        } else {
            StringTreeConcat(newChildren)
        }
    }

}
