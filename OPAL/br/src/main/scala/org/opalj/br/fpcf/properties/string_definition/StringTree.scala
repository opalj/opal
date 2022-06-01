/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.fpcf.properties.string_definition

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.fpcf.properties.properties.StringTree
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation.InfiniteRepetitionSymbol

/**
 * Super type for modeling nodes and leafs of [[org.opalj.br.fpcf.properties.properties.StringTree]]s.
 *
 * @author Patrick Mell
 */
sealed abstract class StringTreeElement(val children: ListBuffer[StringTreeElement]) {

    /**
     * This is a helper function which processes the `reduce` operation for [[StringTreeOr]] and
     * [[StringTreeCond]] elements (as both are processed in a very similar fashion). `children`
     * denotes the children of the [[StringTreeOr]] or [[StringTreeCond]] element and `processOr`
     * defines whether to process [[StringTreeOr]] or [[StringTreeCond]] (the latter in case `false`
     * is passed).
     */
    private def processReduceCondOrReduceOr(
        children: List[StringTreeElement], processOr: Boolean = true
    ): List[StringConstancyInformation] = {
        val reduced = children.flatMap(reduceAcc)
        val resetElement = reduced.find(_.constancyType == StringConstancyType.RESET)
        val replaceElement = reduced.find(_.constancyType == StringConstancyType.REPLACE)
        val appendElements = reduced.filter { _.constancyType == StringConstancyType.APPEND }
        val appendSci = if (appendElements.nonEmpty) {
            Some(appendElements.reduceLeft((o, n) ⇒ StringConstancyInformation(
                StringConstancyLevel.determineMoreGeneral(o.constancyLevel, n.constancyLevel),
                StringConstancyType.APPEND,
                s"${o.possibleStrings}|${n.possibleStrings}"
            )))
        } else {
            None
        }
        val scis = ListBuffer[StringConstancyInformation]()

        if (appendSci.isDefined) {
            // The only difference between a Cond and an Or is how the possible strings look like
            var possibleStrings = s"${appendSci.get.possibleStrings}"
            if (processOr) {
                if (appendElements.tail.nonEmpty) {
                    possibleStrings = s"($possibleStrings)"
                }
            } else {
                possibleStrings = s"(${appendSci.get.possibleStrings})?"
            }
            scis.append(StringConstancyInformation(
                appendSci.get.constancyLevel, appendSci.get.constancyType, possibleStrings
            ))
        }
        if (resetElement.isDefined) {
            scis.append(resetElement.get)
        }
        if (replaceElement.isDefined) {
            scis.append(replaceElement.get)
        }
        scis.toList
    }

    /**
     * This is a helper function which processes the `reduce` operation for [[StringTreeConcat]]
     * elements.
     */
    private def processReduceConcat(
        children: List[StringTreeElement]
    ): List[StringConstancyInformation] = {
        val reducedLists = children.map(reduceAcc)
        // Stores whether we deal with a flat structure or with a nested structure (in the latter
        // case maxNestingLevel >= 2)
        val maxNestingLevel = reducedLists.foldLeft(0) {
            (max: Int, next: List[StringConstancyInformation]) ⇒ Math.max(max, next.size)
        }
        val scis = ListBuffer[StringConstancyInformation]()
        // Stores whether the last processed element was of type RESET
        var wasReset = false

        reducedLists.foreach { nextSciList ⇒
            nextSciList.foreach { nextSci ⇒
                // Add the first element only if not a reset (otherwise no new information)
                if (scis.isEmpty && nextSci.constancyType != StringConstancyType.RESET) {
                    scis.append(nextSci)
                } // No two consecutive resets (as that does not add any new information either)
                else if (!wasReset || nextSci.constancyType != StringConstancyType.RESET) {
                    // A reset / replace marks a new starting point
                    val isReset = nextSci.constancyType == StringConstancyType.RESET
                    val isReplace = nextSci.constancyType == StringConstancyType.REPLACE
                    if (isReset || isReplace) {
                        // maxNestingLevel == 1 corresponds to n consecutive append call, i.e.,
                        // clear everything that has been seen so far
                        if (maxNestingLevel == 1) {
                            scis.clear()
                            // In case of replace, add the replace element
                            if (isReplace) {
                                scis.append(nextSci)
                            }
                        } // maxNestingLevel >= 2 corresponds to a new starting point (e.g., a clear
                        // in a if-else construction) => Add a new element
                        else {
                            scis.append(nextSci)
                        }
                    } // Otherwise, collapse / combine with previous elements
                    else {
                        scis.zipWithIndex.foreach {
                            case (innerSci, index) ⇒
                                val collapsed = StringConstancyInformation(
                                    StringConstancyLevel.determineForConcat(
                                        innerSci.constancyLevel, nextSci.constancyLevel
                                    ),
                                    StringConstancyType.APPEND,
                                    innerSci.possibleStrings + nextSci.possibleStrings
                                )
                                scis(index) = collapsed
                        }
                    }
                }
                // For the next iteration
                wasReset = nextSci.constancyType == StringConstancyType.RESET
            }
        }
        scis.toList
    }

    /**
     * Accumulator / helper function for reducing a tree.
     *
     * @param subtree The (sub) tree to reduce.
     * @return The reduced tree in the form of a list of [[StringConstancyInformation]]. That is, if
     *         different [[StringConstancyType]]s occur, a single StringConstancyInformation element
     *         is not sufficient to describe the string approximation for this function. For
     *         example, a [[StringConstancyType.RESET]] marks the beginning of a new string
     *         alternative which results in a new list element.
     */
    private def reduceAcc(subtree: StringTreeElement): List[StringConstancyInformation] = {
        subtree match {
            case StringTreeRepetition(c, lowerBound, upperBound) ⇒
                val times = if (lowerBound.isDefined && upperBound.isDefined)
                    (upperBound.get - lowerBound.get).toString else InfiniteRepetitionSymbol
                val reducedAcc = reduceAcc(c)
                val reduced = if (reducedAcc.nonEmpty) reducedAcc.head else
                    StringConstancyInformation.lb
                List(StringConstancyInformation(
                    reduced.constancyLevel,
                    reduced.constancyType,
                    s"(${reduced.possibleStrings})$times"
                ))
            case StringTreeConcat(cs) ⇒ processReduceConcat(cs.toList)
            case StringTreeOr(cs)     ⇒ processReduceCondOrReduceOr(cs.toList)
            case StringTreeCond(cs)   ⇒ processReduceCondOrReduceOr(cs.toList, processOr = false)
            case StringTreeConst(sci) ⇒ List(sci)
        }
    }

    /**
     * This function removes duplicate [[StringTreeConst]]s from a given list. In this
     * context, two elements are equal if their [[StringTreeConst.sci]] information are equal.
     *
     * @param children The children from which to remove duplicates.
     * @return Returns a list of [[StringTreeElement]] with unique elements.
     */
    private def removeDuplicateTreeValues(
        children: ListBuffer[StringTreeElement]
    ): ListBuffer[StringTreeElement] = {
        val seen = mutable.Map[StringConstancyInformation, Boolean]()
        val unique = ListBuffer[StringTreeElement]()
        children.foreach {
            case next @ StringTreeConst(sci) ⇒
                if (!seen.contains(sci)) {
                    seen += (sci → true)
                    unique.append(next)
                }
            case loop: StringTreeRepetition ⇒ unique.append(loop)
            case concat: StringTreeConcat   ⇒ unique.append(concat)
            case or: StringTreeOr           ⇒ unique.append(or)
            case cond: StringTreeCond       ⇒ unique.append(cond)
        }
        unique
    }

    /**
     * Accumulator function for simplifying a tree.
     */
    private def simplifyAcc(subtree: StringTree): StringTree = {
        subtree match {
            case StringTreeOr(cs) ⇒
                cs.foreach {
                    case nextC @ StringTreeOr(subChildren) ⇒
                        simplifyAcc(nextC)
                        var insertIndex = subtree.children.indexOf(nextC)
                        subChildren.foreach { next ⇒
                            subtree.children.insert(insertIndex, next)
                            insertIndex += 1
                        }
                        subtree.children.-=(nextC)
                    case _ ⇒
                }
                val unique = removeDuplicateTreeValues(cs)
                subtree.children.clear()
                subtree.children.appendAll(unique)
                subtree
            case stc: StringTreeCond ⇒
                // If the child of a StringTreeCond is a StringTreeRepetition, replace the
                // StringTreeCond by the StringTreeRepetition element (otherwise, regular
                // expressions like ((s)*)+ will follow which is equivalent to (s)*).
                if (stc.children.nonEmpty && stc.children.head.isInstanceOf[StringTreeRepetition]) {
                    stc.children.head
                } else {
                    stc
                }
            // Remaining cases are trivial
            case str: StringTreeRepetition ⇒ StringTreeRepetition(simplifyAcc(str.child))
            case stc: StringTreeConcat     ⇒ StringTreeConcat(stc.children.map(simplifyAcc))
            case stc: StringTreeConst      ⇒ stc
        }
    }

    /**
     * Accumulator function for grouping repetition elements.
     */
    private def groupRepetitionElementsAcc(subtree: StringTree): StringTree = {
        /**
         * Function for processing [[StringTreeOr]] or [[StringTreeConcat]] elements as these cases
         * are equal (except for distinguishing the object to return). Thus, make sure that only
         * instance of these classes are passed. Otherwise, an exception will be thrown!
         */
        def processConcatOrOrCase(subtree: StringTree): StringTree = {
            if (!subtree.isInstanceOf[StringTreeOr] && !subtree.isInstanceOf[StringTreeConcat]) {
                throw new IllegalArgumentException(
                    "can only process instances of StringTreeOr and StringTreeConcat"
                )
            }

            var newChildren = subtree.children.map(groupRepetitionElementsAcc)
            val repetitionElements = newChildren.filter(_.isInstanceOf[StringTreeRepetition])
            // Nothing to do when less than two repetition elements
            if (repetitionElements.length <= 1) {
                // In case there is only one (new) repetition element, replace the children
                subtree.children.clear()
                subtree.children.append(newChildren: _*)
                subtree
            } else {
                val childrenOfReps = repetitionElements.map(
                    _.asInstanceOf[StringTreeRepetition].child
                )
                val newRepElement = StringTreeRepetition(StringTreeOr(childrenOfReps))
                val indexFirstChild = newChildren.indexOf(repetitionElements.head)
                newChildren = newChildren.filterNot(_.isInstanceOf[StringTreeRepetition])
                newChildren.insert(indexFirstChild, newRepElement)
                if (newChildren.length == 1) {
                    newChildren.head
                } else {
                    if (subtree.isInstanceOf[StringTreeOr]) {
                        StringTreeOr(newChildren)
                    } else {
                        StringTreeConcat(newChildren)
                    }
                }
            }
        }

        subtree match {
            case sto: StringTreeOr     ⇒ processConcatOrOrCase(sto)
            case stc: StringTreeConcat ⇒ processConcatOrOrCase(stc)
            case StringTreeCond(cs) ⇒
                StringTreeCond(cs.map(groupRepetitionElementsAcc))
            case StringTreeRepetition(child, _, _) ⇒
                StringTreeRepetition(groupRepetitionElementsAcc(child))
            case stc: StringTreeConst ⇒ stc
        }
    }

    /**
     * Reduces this [[StringTree]] instance to a [[StringConstancyInformation]] object that captures
     * the information stored in this tree.
     *
     * @param preprocess If set to true, `this` tree will be preprocess, i.e., it will be
     *                   simplified and repetition elements be grouped. Note that pre-processing
     *                   changes `this` instance!
     * @return A [[StringConstancyInformation]] instance that flatly describes this tree.
     */
    def reduce(preprocess: Boolean = false): StringConstancyInformation = {
        if (preprocess) {
            simplify().groupRepetitionElements()
        }
        // The call to reduceMultiple is necessary as reduceAcc might return a list, e.g., if a
        // clear occurred
        StringConstancyInformation.reduceMultiple(reduceAcc(this))
    }

    /**
     * Simplifies this tree. Currently, this means that when a (sub) tree has a
     * [[StringTreeCond]] as root, ''r'', and a child, ''c'' (or several children)
     * which is a [[StringTreeCond]] as well, that ''c'' is attached as a direct child
     * of ''r'' (the child [[StringTreeCond]] under which ''c'' was located is then
     * removed safely).
     *
     * @return This function modifies `this` tree and returns this instance, e.g., for chaining
     *         commands.
     * @note Applying this function changes the representation of the tree but not produce a
     *       semantically different tree! Executing this function prior to [[reduce()]] simplifies
     *       its stringified representation.
     */
    def simplify(): StringTree = simplifyAcc(this)

    /**
     * This function groups repetition elements that belong together. For example, an if-else block,
     * which both append to a StringBuilder is modeled as a [[StringTreeOr]] with two
     * [[StringTreeRepetition]] elements. Conceptually, this is not wrong, however, may create
     * confusion when interpreting the tree / expression. This function finds such groupable
     * children and actually groups them.
     *
     * @return This function modifies `this` tree and returns this instance, e.g., for chaining
     *         commands.
     * @note Applying this function changes the representation of the tree but not produce a
     *       semantically different tree!
     */
    def groupRepetitionElements(): StringTree = groupRepetitionElementsAcc(this)

}

/**
 * [[StringTreeRepetition]] models repetitive elements within a [[StringTree]], such as loops
 * or recursion. [[StringTreeRepetition]] are required to have a child. A tree with a
 * [[StringTreeRepetition]] that has no child is regarded as an invalid tree!<br>
 *
 * `lowerBound` and `upperBound` refer to how often the element is repeated / evaluated when run.
 * It may either refer to loop bounds or how often a recursion is repeated. If either or both values
 * is/are set to `None`, it cannot be determined of often the element is actually repeated.
 * Otherwise, the number of repetitions is computed by `upperBound - lowerBound`.
 */
case class StringTreeRepetition(
        var child:  StringTreeElement,
        lowerBound: Option[Int]       = None,
        upperBound: Option[Int]       = None
) extends StringTreeElement(ListBuffer(child))

/**
 * [[StringTreeConcat]] models the concatenation of multiple strings. For example, if it is known
 * that a string is the concatenation of ''s_1'', ..., ''s_n'' (in that order), use a
 * [[StringTreeConcat]] element where the first child / first element in the `children`list
 * represents ''s_1'' and the last child / last element ''s_n''.
 */
case class StringTreeConcat(
        override val children: ListBuffer[StringTreeElement]
) extends StringTreeElement(children)

/**
 * [[StringTreeOr]] models that a string (or part of a string) has one out of several possible
 * values. For instance, if in an `if` block and its corresponding `else` block two values, ''s1''
 * and ''s2'' are appended to a [[StringBuffer]], `sb`, a [[StringTreeOr]] can be used to model that
 * `sb` can contain either ''s1'' or ''s2'' (but not both at the same time!).<br>
 *
 * In contrast to [[StringTreeCond]], [[StringTreeOr]] provides several possible values for
 * a (sub) string.
 */
case class StringTreeOr(
        override val children: ListBuffer[StringTreeElement]
) extends StringTreeElement(children)

/**
 * [[StringTreeCond]] is used to model that a string (or part of a string) is optional / may
 * not always be present. For example, if an `if` block (and maybe a corresponding `else if` but NO
 * `else`) appends to a [[StringBuilder]], a [[StringTreeCond]] is appropriate.<br>
 *
 * In contrast to [[StringTreeOr]], [[StringTreeCond]] provides a way to express that a (sub)
 * string may have (contain) a particular but not necessarily.
 */
case class StringTreeCond(
        override val children: ListBuffer[StringTreeElement]
) extends StringTreeElement(children)

/**
 * [[StringTreeConst]]s are the only elements which are supposed to act as leafs within a
 * [[StringTree]].
 *
 * `sci` is a [[StringConstancyInformation]] instance that resulted from evaluating an
 * expression and that represents part of the value(s) a string may have.
 */
case class StringTreeConst(
        sci: StringConstancyInformation
) extends StringTreeElement(ListBuffer())
