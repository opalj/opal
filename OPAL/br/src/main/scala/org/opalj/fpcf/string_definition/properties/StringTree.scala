/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.string_definition.properties

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

/**
 * Models the nodes and leafs of [[StringTree]].
 * TODO: Prepend "String"
 *
 * @author Patrick Mell
 */
sealed abstract class TreeElement(val children: ListBuffer[TreeElement]) {

    /**
     * Accumulator / helper function for reducing a tree.
     *
     * @param subtree The tree (or subtree) to reduce.
     * @return The reduced tree.
     */
    private def reduceAcc(subtree: TreeElement): StringConstancyInformation = {
        subtree match {
            case TreeConditionalElement(c) ⇒
                val scis = c.map(reduceAcc)
                val reducedInfo = scis.reduceLeft((o, n) ⇒ StringConstancyInformation(
                    StringConstancyLevel.determineMoreGeneral(o.constancyLevel, n.constancyLevel),
                    s"${o.possibleStrings} | ${n.possibleStrings}"
                ))
                StringConstancyInformation(
                    reducedInfo.constancyLevel, s"(${reducedInfo.possibleStrings})"
                )

            case TreeLoopElement(c, nli) ⇒
                val reduced = reduceAcc(c)
                val times = if (nli.isDefined) nli.get.toString else "∞"
                StringConstancyInformation(
                    reduced.constancyLevel,
                    s"(${reduced.possibleStrings})^$times"
                )

            case TreeValueElement(c, sci) ⇒
                c match {
                    case Some(child) ⇒
                        val reduced = reduceAcc(child)
                        // Do not consider an empty constructor as a CONSTANT value (otherwise
                        // PARTIALLY_CONSTANT will result when DYNAMIC is required)
                        val level = if (sci.possibleStrings == "") {
                            reduced.constancyLevel
                        } else {
                            StringConstancyLevel.determineForConcat(
                                sci.constancyLevel, reduced.constancyLevel
                            )
                        }
                        StringConstancyInformation(
                            level, sci.possibleStrings + reduced.possibleStrings
                        )
                    case None ⇒ sci
                }
        }
    }

    /**
     * This function removes duplicate [[TreeValueElement]]s from a given list. In this context, two
     * elements are equal if their [[TreeValueElement.sci]] information is equal.
     *
     * @param children The children from which to remove duplicates.
     * @return Returns a list of [[TreeElement]] with unique elements.
     */
    private def removeDuplicateTreeValues(
        children: ListBuffer[TreeElement]
    ): ListBuffer[TreeElement] = {
        val seen = mutable.Map[StringConstancyInformation, Boolean]()
        val unique = ListBuffer[TreeElement]()
        children.foreach {
            case next@TreeValueElement(_, sci) ⇒
                if (!seen.contains(sci)) {
                    seen += (sci → true)
                    unique.append(next)
                }
            case loopElement: TreeLoopElement ⇒ unique.append(loopElement)
            case condElement: TreeConditionalElement ⇒ unique.append(condElement)
        }
        unique
    }

    /**
     * Accumulator function for simplifying a tree.
     */
    private def simplifyAcc(subtree: StringTree): StringTree = {
        subtree match {
            case TreeConditionalElement(cs) ⇒
                cs.foreach {
                    case nextC@TreeConditionalElement(subChildren) ⇒
                        simplifyAcc(nextC)
                        subChildren.foreach(subtree.children.append(_))
                        subtree.children.-=(nextC)
                    case _ ⇒
                }
                val unique = removeDuplicateTreeValues(cs)
                subtree.children.clear()
                subtree.children.appendAll(unique)
                subtree
            case _ ⇒ subtree
        }
    }

    /**
     * Reduces this [[StringTree]] instance to a [[StringConstancyInformation]] object that captures
     * the information stored in this tree.
     *
     * @return A [[StringConstancyInformation]] instance that flatly describes this tree.
     */
    def reduce(): StringConstancyInformation = reduceAcc(this)

    /**
     * Simplifies this tree. Currently, this means that when a (sub) tree has a
     * [[TreeConditionalElement]] as root, ''r'', and a child, ''c'' (or several children) which is
     * a [[TreeConditionalElement]] as well, that ''c'' is attached as a direct child of ''r'' (the
     * child [[TreeConditionalElement]] under which ''c'' was located is then removed safely).
     *
     * @return This function modifies `this` tree and returns this instance, e.g., for chaining
     *         commands.
     * @note Applying this function changes the representation of the tree but not produce a
     *       semantically different tree! Executing this function prior to [[reduce()]] simplifies
     *       its stringified representation.
     */
    def simplify(): StringTree = simplifyAcc(this)

    /**
     * @return Returns all leaf elements of this instance.
     */
    def getLeafs: Array[TreeValueElement] = {
        def leafsAcc(root: TreeElement, leafs: ArrayBuffer[TreeValueElement]): Unit = {
            root match {
                case TreeLoopElement(c, _) ⇒ leafsAcc(c, leafs)
                case TreeConditionalElement(cs) ⇒ cs.foreach(leafsAcc(_, leafs))
                case TreeValueElement(c, _) ⇒
                    if (c.isDefined) {
                        leafsAcc(c.get, leafs)
                    } else {
                        leafs.append(root.asInstanceOf[TreeValueElement])
                    }
            }
        }

        val leafs = ArrayBuffer[TreeValueElement]()
        leafsAcc(this, leafs)
        leafs.toArray
    }

}

/**
 * TreeLoopElement models loops with a [[StringTree]]. `TreeLoopElement`s are supposed to have
 * either at lease one other `TreeLoopElement`, `TreeConditionalElement`, or a [[TreeValueElement]]
 * as children . A tree with a `TreeLoopElement` that has no children is regarded as an invalid tree
 * in this sense!<br>
 *
 * `numLoopIterations` indicates how often the loop iterates. For some loops, this can be statically
 * computed - in this case set `numLoopIterations` to that value. When the number of loop iterations
 * cannot be determined, set it to [[None]].
 */
case class TreeLoopElement(
    child: TreeElement,
    numLoopIterations: Option[Int]
) extends TreeElement(ListBuffer(child))

/**
 * For modelling conditionals, such as if, if-else, if-elseif-else, switch, and also as parent
 * element for possible array values, but no loops! Even though loops are conditionals as well,
 * they are to be modelled using [[TreeLoopElement]] (as they capture further information).<br>
 *
 * `TreeConditionalElement`s are supposed to have either at lease one other
 * `TreeConditionalElement`, `TreeLoopElement`, or a [[TreeValueElement]] as children . A tree with
 * a `TreeConditionalElement` that has no children is regarded as an invalid tree in this sense!
 */
case class TreeConditionalElement(
    override val children: ListBuffer[TreeElement],
) extends TreeElement(children)

/**
 * TreeExprElement are the only elements which are supposed to act as leafs within a
 * [[StringTree]].
 * They may have one `child` but do not need to have children necessarily. Intuitively, a
 * TreeExprElement, ''e1'', which has a child ''e2'', represents the concatenation of ''e1'' and
 * ''e2''.<br>
 *
 * `sci` is a [[StringConstancyInformation]] instance that resulted from evaluating an
 * expression.
 */
case class TreeValueElement(
    var child: Option[TreeElement],
    sci: StringConstancyInformation
) extends TreeElement(
    child match {
        case Some(c) ⇒ ListBuffer(c)
        case None ⇒ ListBuffer()
    }
)
