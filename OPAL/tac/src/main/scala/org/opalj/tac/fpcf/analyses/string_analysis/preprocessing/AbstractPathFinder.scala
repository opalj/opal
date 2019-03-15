/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.preprocessing

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CatchNode
import org.opalj.br.cfg.CFG
import org.opalj.br.cfg.CFGNode
import org.opalj.tac.Goto
import org.opalj.tac.If
import org.opalj.tac.ReturnValue
import org.opalj.tac.Stmt
import org.opalj.tac.Switch
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * [[AbstractPathFinder]] provides a scaffolding for finding all relevant paths in a CFG in the
 * scope of string definition analyses.
 *
 * @param cfg The control flow graph (CFG) on which instance of this class will operate on.
 *
 * @author Patrick Mell
 */
abstract class AbstractPathFinder(cfg: CFG[Stmt[V], TACStmts[V]]) {

    /**
     * CSInfo stores information regarding control structures (CS) in the form: Index of the start
     * statement of that CS, index of the end statement of that CS and the type.
     */
    protected type CSInfo = (Int, Int, NestedPathType.Value)

    /**
     * Represents control structures in a hierarchical order. The top-most level of the hierarchy
     * has no [[CSInfo]], thus value can be set to `None`; all other elements are required to have
     * that value set!
     *
     * @param hierarchy A list of pairs where the first element represents the parent and the second
     *                  the list of children. As the list of children is of type
     *                  [[HierarchicalCSOrder]], too, this creates a recursive structure.
     *                  If two elements, ''e1'' and ''e2'', are on the same hierarchy level neither
     *                  ''e1'' is a parent or child of ''e'' and nor is ''e2'' a parent or child of
     *                  ''e1''.
     */
    protected case class HierarchicalCSOrder(
            hierarchy: List[(Option[CSInfo], List[HierarchicalCSOrder])]
    )

    /**
     * Determines the bounds of a conditional with alternative (like an `if-else` or a `switch` with
     * a `default` case, that is the indices of the first and the last statement belonging to the
     * whole block (i.e., for an `if-else` this function returns the index of the very first
     * statement of the `if`, including the branching site, as the first value and the index of the
     * very last element of the `else` part as the second value).
     *
     * @param branchingSite The `branchingSite` is supposed to point at the very first `if` of the
     *                      conditional.
     * @param processedIfs A map which will be filled with the `if` statements that will be
     *                     encountered during the processing. This might be relevant for a method
     *                     processing all `if`s - the `if` of an `else-if` is shall probably be
     *                     processed only once. This map can be used for that purpose.
     * @return Returns the index of the start statement and the index of the end statement of the
     *         whole conditional as described above.
     */
    private def getStartAndEndIndexOfCondWithAlternative(
        branchingSite: Int, processedIfs: mutable.Map[Int, Unit.type]
    ): (Int, Int) = {
        processedIfs(branchingSite) = Unit

        var endSite = -1
        val stack = mutable.Stack[Int](branchingSite)
        while (stack.nonEmpty) {
            val popped = stack.pop()
            val nextBlock = cfg.bb(popped).successors.map {
                case bb: BasicBlock ⇒ bb.startPC
                // Handle Catch Nodes?
                case _              ⇒ -1
            }.max
            var containsIf = false
            for (i ← cfg.bb(nextBlock).startPC.to(cfg.bb(nextBlock).endPC)) {
                if (cfg.code.instructions(i).isInstanceOf[If[V]]) {
                    processedIfs(i) = Unit
                    containsIf = true
                }
            }

            if (containsIf) {
                stack.push(nextBlock)
            } else {
                // Check and find if there is a goto which provides further information about the
                // bounds of the conditional; a goto is relevant, if it does not point back at a
                // surrounding loop
                var isRelevantGoto = false
                val relevantGoTo: Option[Goto] = cfg.code.instructions(nextBlock - 1) match {
                    case goto: Goto ⇒
                        // A goto is not relevant if it points at a loop that is within the
                        // conditional (this does not help / provides no further information)
                        val gotoSite = goto.targetStmt
                        isRelevantGoto = !cfg.findNaturalLoops().exists { l ⇒
                            l.head == gotoSite
                        }
                        Some(goto)
                    case _ ⇒ None
                }

                relevantGoTo match {
                    case Some(goto) ⇒
                        if (isRelevantGoto) {
                            // Find the goto that points after the "else" part (the assumption is
                            // that this goto is the very last element of the current branch
                            endSite = goto.targetStmt
                            // The goto might point back at the beginning of a loop; if so, the end
                            // of the if/else is denoted by the end of the loop
                            if (endSite < branchingSite) {
                                endSite = cfg.findNaturalLoops().filter(_.head == endSite).head.last
                            } else {
                                endSite -= 1
                            }
                        } else {
                            // If the conditional is encloses in a try-catch block, consider this
                            // bounds and otherwise the bounds of the surrounding element
                            cfg.bb(nextBlock).successors.find(_.isInstanceOf[CatchNode]) match {
                                case Some(cs: CatchNode) ⇒
                                    endSite = cs.endPC
                                    if (endSite == -1) {
                                        endSite = nextBlock
                                    }
                                case _ ⇒
                                    endSite = if (nextBlock > branchingSite) nextBlock - 1 else
                                        cfg.findNaturalLoops().find {
                                            _.head == goto.targetStmt
                                        }.get.last
                            }
                        }
                    case _ ⇒
                        // No goto available => Jump after next block
                        var nextIf: Option[If[V]] = None
                        var i = nextBlock
                        while (i < cfg.code.instructions.length && nextIf.isEmpty) {
                            cfg.code.instructions(i) match {
                                case iff: If[V] ⇒
                                    nextIf = Some(iff)
                                    processedIfs(i) = Unit
                                case _ ⇒
                            }
                            i += 1
                        }
                        endSite = if (nextIf.isDefined) nextIf.get.targetStmt else {
                            stack.clear()
                            i - 1
                        }
                }
            }
            if (endSite < branchingSite) {
                endSite = nextBlock
            }
        }

        (branchingSite, endSite)
    }

    /**
     * Determines the bounds of a conditional without alternative (like an `if-else-if` or a
     * `switch` without a `default` case, that is the indices of the first and the last statement
     * belonging to the whole block (i.e., for an `if-else-if` this function returns the index of
     * the very first statement of the `if`, including the branching site, as the first value and
     * the index of the very last element of the `else if` part as the second value).
     *
     * @param branchingSite The `branchingSite` is supposed to point at the very first `if` of the
     *                      conditional.
     * @param processedIfs  A map which will be filled with the `if` statements that will be
     *                      encountered during the processing. This might be relevant for a method
     *                      processing all `if`s - the `if` of an `else-if` is shall probably be
     *                      processed only once. This map can be used for that purpose.
     * @return Returns the index of the start statement and the index of the end statement of the
     *         whole conditional as described above.
     */
    private def getStartAndEndIndexOfCondWithoutAlternative(
        branchingSite: Int, processedIfs: mutable.Map[Int, Unit.type]
    ): (Int, Int) = {
        // Find the index of very last element in the if block (here: The goto element; is it always
        // present?)
        val nextPossibleIfBlock = cfg.bb(branchingSite).successors.map {
            case bb: BasicBlock ⇒ bb.startPC
            // Handle Catch Nodes?
            case _              ⇒ -1
        }.max

        var nextIfIndex = -1
        val ifTarget = cfg.code.instructions(branchingSite).asInstanceOf[If[V]].targetStmt
        for (i ← cfg.bb(nextPossibleIfBlock).startPC.to(cfg.bb(nextPossibleIfBlock).endPC)) {
            // The second condition is necessary to detect two consecutive "if"s (not in an else-if
            // relation)
            if (cfg.code.instructions(i).isInstanceOf[If[V]] && ifTarget != i) {
                nextIfIndex = i
            }
        }

        var endIndex = nextPossibleIfBlock - 1
        if (nextIfIndex > -1 && !isHeadOfLoop(nextIfIndex, cfg.findNaturalLoops(), cfg)) {
            processedIfs(nextIfIndex) = Unit
            val (_, newEndIndex) = getStartAndEndIndexOfCondWithoutAlternative(
                nextIfIndex, processedIfs
            )
            endIndex = newEndIndex
        }

        // It might be that the "i"f is the very last element in a loop; in this case, it is a
        // little bit more complicated to find the end of the "if": Go up to the element that points
        // to the if target element
        if (ifTarget < branchingSite) {
            val seenElements: mutable.Map[Int, Unit] = mutable.Map()
            val toVisit = mutable.Stack[Int](branchingSite)
            while (toVisit.nonEmpty) {
                val popped = toVisit.pop()
                seenElements(popped) = Unit
                val relevantSuccessors = cfg.bb(popped).successors.filter {
                    _.isInstanceOf[BasicBlock]
                }.map(_.asBasicBlock)
                if (relevantSuccessors.size == 1 && relevantSuccessors.head.startPC == ifTarget) {
                    endIndex = cfg.bb(popped).endPC
                    toVisit.clear()
                } else {
                    toVisit.pushAll(relevantSuccessors.filter { s ⇒
                        s.nodeId != ifTarget && !seenElements.contains(s.nodeId)
                    }.map(_.startPC))
                }
            }
        }

        // It might be that this conditional is within a try block. In that case, endIndex will
        // point after all catch clauses which is to much => narrow down to try block
        val inTryBlocks = cfg.catchNodes.filter { cn ⇒
            branchingSite >= cn.startPC && branchingSite <= cn.endPC
        }
        if (inTryBlocks.nonEmpty) {
            val tryEndPC = inTryBlocks.minBy(-_.startPC).endPC
            if (endIndex > tryEndPC) {
                endIndex = tryEndPC
            }
        }

        // It is now necessary to collect all ifs that belong to the whole if condition (in the
        // high-level construct)
        cfg.bb(ifTarget).predecessors.foreach {
            case pred: BasicBlock ⇒
                for (i ← pred.startPC.to(pred.endPC)) {
                    if (cfg.code.instructions(i).isInstanceOf[If[V]]) {
                        processedIfs(i) = Unit
                    }
                }
            // How about CatchNodes?
            case _ ⇒
        }

        (branchingSite, endIndex)
    }

    /**
     * This method finds the very first return value after (including) the given start position.
     *
     * @param startPos The index of the position to start with.
     * @return Returns either the index of the very first found [[ReturnValue]] or the index of the
     *         very last statement within the instructions if no [[ReturnValue]] could be found.
     */
    private def findNextReturn(startPos: Int): Int = {
        var returnPos = startPos
        var foundReturn = false
        while (!foundReturn && returnPos < cfg.code.instructions.length) {
            if (cfg.code.instructions(returnPos).isInstanceOf[ReturnValue[V]]) {
                foundReturn = true
            } else {
                returnPos += 1
            }
        }
        returnPos
    }

    /**
     * This function detects all `try-catch` blocks in the given CFG, extracts the indices of the
     * first statement for each `try` as the as well as the indices of the last statements of the
     * `try-catch` blocks and returns these pairs (along with [[NestedPathType.TryCatchFinally]].
     *
     * @return Returns information on all `try-catch` blocks present in the given `cfg`.
     *
     * @note The bounds, which are determined by this function do not include the `finally` part of
     *       `try` blocks (but for the `catch` blocks). Thus, a function processing the result of
     *       this function can either add the `finally` to the `try` block (and keep it in the
     *       `catch` block(s)) or add it after the whole `try-catch` but disregards it for all
     *       `catch` blocks.
     * @note This function has basic support for `throwable`s.
     */
    private def determineTryCatchBounds(): List[CSInfo] = {
        // Stores the startPC as key and the index of the end of a catch (or finally if it is
        // present); a map is used for faster accesses
        val tryInfo = mutable.Map[Int, Int]()

        cfg.catchNodes.foreach { cn ⇒
            if (!tryInfo.contains(cn.startPC)) {
                val cnSameStartPC = cfg.catchNodes.filter(_.startPC == cn.startPC)
                val hasCatchFinally = cnSameStartPC.exists(_.catchType.isEmpty)
                val hasOnlyFinally = cnSameStartPC.size == 1 && hasCatchFinally
                val isThrowable = cn.catchType.isDefined &&
                    cn.catchType.get.fqn == "java/lang/Throwable"
                // When there is a throwable involved, it might be the case that there is only one
                // element in cnSameStartPC, the finally part; do not process it now (but in another
                // catch node)
                if (!hasOnlyFinally) {
                    if (isThrowable) {
                        val throwFinally = cfg.catchNodes.find(_.startPC == cn.handlerPC)
                        val endIndex = if (throwFinally.isDefined) throwFinally.get.endPC - 1 else
                            cn.endPC - 1
                        tryInfo(cn.startPC) = endIndex
                    } // If there is only one CatchNode for a startPC, i.e., no finally, no other
                    // catches, the end index can be directly derived from the successors
                    else if (cnSameStartPC.tail.isEmpty && !isThrowable) {
                        if (cn.endPC > -1) {
                            var end = cfg.bb(cn.endPC).successors.map {
                                case bb: BasicBlock ⇒ bb.startPC - 1
                                case _              ⇒ -1
                            }.max
                            if (end == -1) {
                                end = findNextReturn(cn.handlerPC)
                            }
                            tryInfo(cn.startPC) = end
                        } // -1 might be the case if the catch returns => Find that return and use
                        // it as the end of the range
                        else {
                            findNextReturn(cn.handlerPC)
                        }
                    } // Otherwise, the index after the try and all catches marks the end index (-1
                    // to not already get the start index of the successor)
                    else {
                        if (hasCatchFinally) {
                            // Find out, how many elements the finally block has and adjust the try
                            // block accordingly
                            val startFinally = cnSameStartPC.map(_.handlerPC).max
                            val endFinally = cfg.code.instructions(startFinally - 1) match {
                                // If the finally does not terminate a method, it has a goto to jump
                                // after the finally block; if not, the end of the finally is marked
                                // by the end of the method
                                case Goto(_, target) ⇒ target
                                case _               ⇒ cfg.code.instructions.length - 1
                            }
                            val numElementsFinally = endFinally - startFinally - 1
                            val endOfFinally = cnSameStartPC.map(_.handlerPC).max
                            tryInfo(cn.startPC) = endOfFinally - 1 - numElementsFinally
                        } else {
                            val blockIndex = if (cnSameStartPC.head.endPC < 0)
                                cfg.code.instructions.length - 1 else cnSameStartPC.head.endPC
                            tryInfo(cn.startPC) = cfg.bb(blockIndex).successors.map {
                                case bb: BasicBlock ⇒ bb.startPC
                                case _              ⇒ blockIndex
                            }.max - 1
                        }
                    }
                }
            }
        }

        tryInfo.map {
            case (key, value) ⇒ (key, value, NestedPathType.TryCatchFinally)
        }.toList
    }

    /**
     * This function serves as a helper / accumulator function that builds the recursive hierarchy
     * for a given element.
     *
     * @param element  The element for which a hierarchy is to be built.
     * @param children Maps from parent elements ([[CSInfo]]) to its children. `children` is
     *                 supposed to contain all known parent-children relations in order to guarantee
     *                 that the recursive calls will produce a correct result as well).
     * @return The hierarchical structure for `element`.
     */
    private def buildHierarchy(
        element:  CSInfo,
        children: mutable.Map[CSInfo, ListBuffer[CSInfo]]
    ): HierarchicalCSOrder = {
        if (!children.contains(element)) {
            // Recursion anchor (no children available
            HierarchicalCSOrder(List((Some(element), List())))
        } else {
            HierarchicalCSOrder(List((
                Some(element), children(element).map { buildHierarchy(_, children) }.toList
            )))
        }
    }

    /**
     * This function builds a [[Path]] that consists of a single [[NestedPathElement]] of type
     * [[NestedPathType.Repetition]]. If `fill` is set to `true`, the nested path element will be
     * filled with [[FlatPathElement]] ranging from `start` to `end` (otherwise, the nested path
     * element remains empty and is to be filled outside this method).
     * This method returns the [[Path]] element along with a list of a single element that consists
     * of the tuple `(start, end)`.
     */
    private def buildRepetitionPath(
        start: Int, end: Int, fill: Boolean
    ): (Path, List[(Int, Int)]) = {
        val path = ListBuffer[SubPath]()
        if (fill) {
            start.to(end).foreach(i ⇒ path.append(FlatPathElement(i)))
        }
        (Path(List(NestedPathElement(path, Some(NestedPathType.Repetition)))), List((start, end)))
    }

    /**
     * This function builds the [[Path]] element for conditionals with and without alternatives
     * (e.g., `if`s that have an `else` block or not); which one is determined by `pathType`.
     * `start` and `end` determine the start and end index of the conditional (`start` is supposed
     * to contain the initial branching site of the conditionals).
     * This function determines all `if`, `else-if`, and `else` blocks and adds them to the path
     * element that will be returned. If `fill` is set to `true`, the different parts will be filled
     * with [[FlatPathElement]]s.
     * For example, assume an `if-else` where the `if` start at index 5, ends at index 10, and the
     * `else` part starts at index 11 and ends at index 20. [[Path]] will then contain a
     * [[NestedPathElement]] of type [[NestedPathType.CondWithAlternative]] with two children. If
     * `fill` equals `true`, the first inner path will contain flat path elements from 5 to 10 and
     * the second from 11 to 20.
     */
    private def buildCondPath(
        start: Int, end: Int, pathType: NestedPathType.Value, fill: Boolean
    ): (Path, List[(Int, Int)]) = {
        // Stores the start and end indices of the parts that form the if-(else-if)*-else, i.e., if
        // there is an if-else construct, startEndPairs contains two elements: 1) The start index of
        // the if, the end index of the if part and 2) the start index of the else part and the end
        // index of the else part
        val startEndPairs = ListBuffer[(Int, Int)]()

        var endSite = -1
        val stack = mutable.Stack[Int](start)
        while (stack.nonEmpty) {
            val popped = stack.pop()
            if (popped <= end) {
                var nextBlock = cfg.bb(popped).successors.map {
                    case bb: BasicBlock ⇒ bb.startPC
                    // Handle Catch Nodes?
                    case _              ⇒ -1
                }.max

                if (pathType == NestedPathType.CondWithAlternative && nextBlock > end) {
                    nextBlock = popped + 1
                    while (nextBlock < cfg.code.instructions.length - 1 &&
                        !cfg.code.instructions(nextBlock).isInstanceOf[If[V]]) {
                        nextBlock += 1
                    }
                }

                var containsIf = false
                for (i ← cfg.bb(nextBlock).startPC.to(cfg.bb(nextBlock).endPC)) {
                    if (cfg.code.instructions(i).isInstanceOf[If[V]]) {
                        containsIf = true
                    }
                }

                if (containsIf) {
                    startEndPairs.append((popped, nextBlock - 1))
                    stack.push(nextBlock)
                } else {
                    if (popped <= end) {
                        endSite = nextBlock - 1
                        if (endSite == start) {
                            endSite = end
                        } // The following is necessary to not exceed bounds (might be the case
                        // within a try block for example)
                        else if (endSite > end) {
                            endSite = end
                        }
                        startEndPairs.append((popped, endSite))
                    }
                }
            }
        }

        // Append the "else" branch (if present)
        if (pathType == NestedPathType.CondWithAlternative && startEndPairs.last._2 + 1 <= end) {
            startEndPairs.append((startEndPairs.last._2 + 1, end))
        }

        val subPaths = ListBuffer[SubPath]()
        startEndPairs.foreach {
            case (startSubpath, endSubpath) ⇒
                val subpathElements = ListBuffer[SubPath]()
                if (fill) {
                    subpathElements.appendAll(startSubpath.to(endSubpath).map(FlatPathElement))
                }
                if (!fill || subpathElements.nonEmpty)
                    subPaths.append(NestedPathElement(subpathElements, None))
        }

        val pathTypeToUse = if (pathType == NestedPathType.CondWithAlternative &&
            startEndPairs.length == 1) NestedPathType.CondWithoutAlternative else pathType

        (Path(List(NestedPathElement(subPaths, Some(pathTypeToUse)))), startEndPairs.toList)
    }

    /**
     * This function works analogously to [[buildCondPath]] only that it processes [[Switch]]
     * statements and that it determines itself whether the switch contains a default case or not.
     */
    private def buildPathForSwitch(
        start: Int, end: Int, fill: Boolean
    ): (Path, List[(Int, Int)]) = {
        val startEndPairs = ListBuffer[(Int, Int)]()
        val switch = cfg.code.instructions(start).asSwitch
        val caseStmts = ListBuffer[Int](switch.caseStmts.sorted: _*)

        val containsDefault = caseStmts.length == caseStmts.distinct.length
        if (containsDefault) {
            caseStmts.append(switch.defaultStmt)
        }
        val pathType = if (containsDefault) NestedPathType.CondWithAlternative else
            NestedPathType.CondWithoutAlternative

        var previousStart = caseStmts.head
        caseStmts.tail.foreach { nextStart ⇒
            val currentEnd = nextStart - 1
            if (currentEnd >= previousStart) {
                startEndPairs.append((previousStart, currentEnd))
            }
            previousStart = nextStart
        }
        if (previousStart <= end) {
            startEndPairs.append((previousStart, end))
        }

        val subPaths = ListBuffer[SubPath]()
        startEndPairs.foreach {
            case (startSubpath, endSubpath) ⇒
                val subpathElements = ListBuffer[SubPath]()
                subPaths.append(NestedPathElement(subpathElements, None))
                if (fill) {
                    subpathElements.appendAll(startSubpath.to(endSubpath).map(FlatPathElement))
                }
        }
        (Path(List(NestedPathElement(subPaths, Some(pathType)))), startEndPairs.toList)
    }

    /**
     * This function works analogously to [[buildCondPath]], i.e., it determines the start and end
     * index of the `catch` block and the start and end indices of the `catch` blocks (if present).
     *
     * @note Note that the built path has the following properties: The end index for the `try`
     *       block excludes the `finally` part if it is present; the same applies to the `catch`
     *       blocks! However, the `finally` block is inserted after the [[NestedPathElement]], i.e.,
     *       the path produced by this function contains more than one element (if a `finally`
     *       block is present; this is handled by this function as well).
     *
     * @note This function has basic / primitive support for `throwable`s.
     */
    private def buildTryCatchPath(
        start: Int, end: Int, fill: Boolean
    ): (Path, List[(Int, Int)]) = {
        // For a description, see the comment of this variable in buildCondPath
        val startEndPairs = ListBuffer[(Int, Int)]()

        var catchBlockStartPCs = ListBuffer[Int]()
        var hasFinallyBlock = false
        var throwableElement: Option[CatchNode] = None
        cfg.bb(start).successors.foreach {
            case cn: CatchNode ⇒
                // Add once for the try block
                if (startEndPairs.isEmpty) {
                    val endPC = if (cn.endPC >= 0) cn.endPC else cn.handlerPC
                    startEndPairs.append((cn.startPC, endPC))
                }
                if (cn.catchType.isDefined && cn.catchType.get.fqn == "java/lang/Throwable") {
                    throwableElement = Some(cn)
                } else {
                    catchBlockStartPCs.append(cn.handlerPC)
                    if (cn.startPC == start && cn.catchType.isEmpty) {
                        hasFinallyBlock = true
                    }
                }
            case _ ⇒
        }

        if (throwableElement.isDefined) {
            val throwCatch = cfg.catchNodes.find(_.startPC == throwableElement.get.handlerPC)
            if (throwCatch.isDefined) {
                // This is for the catch block
                startEndPairs.append((throwCatch.get.startPC, throwCatch.get.endPC - 1))
            }
        } else if (startEndPairs.nonEmpty) {
            var numElementsFinally = 0
            if (hasFinallyBlock) {
                // Find out, how many elements the finally block has
                val startFinally = catchBlockStartPCs.max
                val endFinally = cfg.code.instructions(startFinally - 1) match {
                    // If the finally does not terminate a method, it has a goto to jump
                    // after the finally block; if not, the end of the finally is marked
                    // by the end of the method
                    case Goto(_, target) ⇒ target
                    case _               ⇒ cfg.code.instructions.length - 1
                }
                // -1 for unified processing further down below (because in
                // catchBlockStartPCs.foreach, 1 is subtracted)
                numElementsFinally = endFinally - startFinally - 1
            } else {
                val endOfAfterLastCatch = cfg.bb(startEndPairs.head._2).successors.map {
                    case bb: BasicBlock ⇒ bb.startPC
                    case _              ⇒ -1
                }.max
                catchBlockStartPCs.append(endOfAfterLastCatch)
            }

            catchBlockStartPCs = catchBlockStartPCs.sorted
            catchBlockStartPCs.zipWithIndex.foreach {
                case (nextStart, i) ⇒
                    if (i + 1 < catchBlockStartPCs.length) {
                        startEndPairs.append(
                            (nextStart, catchBlockStartPCs(i + 1) - 1 - numElementsFinally)
                        )
                    }
            }
        } // In some cases (sometimes when a throwable is involved) the successors are no catch
        // nodes => Find the bounds now
        else {
            val cn = cfg.catchNodes.filter(_.startPC == start).head
            startEndPairs.append((cn.startPC, cn.endPC - 1))
            val endOfCatch = cfg.code.instructions(cn.handlerPC - 1) match {
                case goto: Goto ⇒
                    // The first statement after the catches; it might be less than cn.startPC in
                    // case it refers to a loop. If so, use the "if" to find the end
                    var indexFirstAfterCatch = goto.targetStmt
                    if (indexFirstAfterCatch < cn.startPC) {
                        var iff: Option[If[V]] = None
                        var i = indexFirstAfterCatch
                        while (iff.isEmpty) {
                            cfg.code.instructions(i) match {
                                case foundIf: If[V] ⇒ iff = Some(foundIf)
                                case _              ⇒
                            }
                            i += 1
                        }
                        indexFirstAfterCatch = iff.get.targetStmt
                    }
                    indexFirstAfterCatch
                case _ ⇒ findNextReturn(cn.handlerPC)
            }
            startEndPairs.append((cn.endPC, endOfCatch))
        }

        val subPaths = ListBuffer[SubPath]()
        startEndPairs.foreach {
            case (startSubpath, endSubpath) ⇒
                val subpathElements = ListBuffer[SubPath]()
                subPaths.append(NestedPathElement(subpathElements, None))
                if (fill) {
                    subpathElements.appendAll(startSubpath.to(endSubpath).map(FlatPathElement))
                }
        }

        // If there is a finally part, append everything after the end of the try block up to the
        // very first catch block
        if (hasFinallyBlock && fill) {
            subPaths.appendAll((startEndPairs.head._2 + 1).until(startEndPairs(1)._1).map { i ⇒
                FlatPathElement(i)
            })
        }

        (
            Path(List(NestedPathElement(subPaths, Some(NestedPathType.TryCatchFinally)))),
            startEndPairs.toList
        )
    }

    /**
     * Generates a new [[NestedPathElement]] with a given number of inner [[NestedPathElement]]s.
     */
    protected def generateNestPathElement(
        numInnerElements: Int,
        elementType:      NestedPathType.Value
    ): NestedPathElement = {
        val outerNested = NestedPathElement(ListBuffer(), Some(elementType))
        for (_ ← 0.until(numInnerElements)) {
            outerNested.element.append(NestedPathElement(ListBuffer(), None))
        }
        outerNested
    }

    /**
     * Determines whether a given `site` is the head of a loop by comparing it to a set of loops
     * (here a list of lists). This function returns ''true'', if `site` is the head of one of the
     * inner lists.
     * Note that some high-level constructs, such as ''while-true'', might produce a loop where the
     * check, whether to loop again or leave the loop, is placed at the end of the loop. In such
     * cases, the very first statement of a loop is considered its head (which can be an assignment
     * or function call not related to the loop header for instance).
     */
    protected def isHeadOfLoop(
        site: Int, loops: List[List[Int]], cfg: CFG[Stmt[V], TACStmts[V]]
    ): Boolean = {
        var belongsToLoopHeader = false

        // First, check the trivial case: Is the given site the first statement in a loop (covers,
        // e.g., the above-mentioned while-true cases)
        loops.foreach { loop ⇒
            if (!belongsToLoopHeader) {
                if (loop.head == site) {
                    belongsToLoopHeader = true
                }
            }
        }

        // The loop header might not only consist of the very first element in 'loops'; thus, check
        // whether the given site is between the first site of a loop and the site of the very first
        // 'if' (again, respect structures as produces by while-true loops)
        if (!belongsToLoopHeader) {
            loops.foreach { nextLoop ⇒
                if (!belongsToLoopHeader) {
                    val start = nextLoop.head
                    var end = start
                    while (!cfg.code.instructions(end).isInstanceOf[If[V]]) {
                        end += 1
                    }
                    if (site >= start && site <= end && end < nextLoop.last) {
                        belongsToLoopHeader = true
                    }
                }
            }
        }
        belongsToLoopHeader
    }

    /**
     * Determines whether a given `site` is the end of a loop by comparing it to a set of loops
     * (here a list of lists). This function returns ''true'', if `site` is the last element of one
     * of the inner lists.
     */
    protected def isEndOfLoop(site: Int, loops: List[List[Int]]): Boolean =
        loops.foldLeft(false)((old: Boolean, nextLoop: List[Int]) ⇒ old || nextLoop.last == site)

    /**
     * Checks whether a given [[BasicBlock]] has one (or several) successors which have at least n
     * predecessors.
     *
     * @param bb The basic block to check whether it has a successor with at least n predecessors.
     * @param n The number of required predecessors.
     * @return Returns ''true'' if ''bb'' has a successor which has at least ''n'' predecessors.
     *
     * @note This function regards as successors and predecessors only [[BasicBlock]]s.
     */
    protected def hasSuccessorWithAtLeastNPredecessors(bb: BasicBlock, n: Int = 2): Boolean =
        bb.successors.filter(
            _.isInstanceOf[BasicBlock]
        ).foldLeft(false)((prev: Boolean, next: CFGNode) ⇒ {
                prev || (next.predecessors.count(_.isInstanceOf[BasicBlock]) >= n)
            })

    /**
     * This function checks if a branching corresponds to an if (or if-elseif) structure that has no
     * else block.
     * Currently, this function is implemented to check whether the very last element of the
     * successors of the given site is a path past the if (or if-elseif) paths.
     *
     * @param branchingSite The site / index of a branching that is to be checked.
     * @param cfg The control flow graph underlying the successors.
     * @return Returns ''true'', if the very last element of the successors is a child of one of the
     *         other successors. If this is the case, the branching corresponds to one without an
     *         ''else'' branch.
     */
    protected def isCondWithoutElse(
        branchingSite: Int,
        cfg:           CFG[Stmt[V], TACStmts[V]],
        processedIfs:  mutable.Map[Int, Unit.type]
    ): Boolean = {
        val successorBlocks = cfg.bb(branchingSite).successors
        // CatchNode exists => Regard it as conditional without alternative
        if (successorBlocks.exists(_.isInstanceOf[CatchNode])) {
            processedIfs(branchingSite) = Unit
            return false
        }

        val successors = successorBlocks.map(_.nodeId).toArray.sorted

        // In case, there is only one larger successor, this will be a condition without else
        // (smaller indices might arise, e.g., when an "if" is the last part of a loop)
        if (successors.count(_ > branchingSite) == 1) {
            return true
        }

        // Separate the last element from all previous ones
        //val branches = successors.reverse.tail.reverse
        val lastEle = successors.last

        // If an "if" ends at the end of a loop (the "if" must be within that loop!), it cannot have
        // an else
        val loopOption = cfg.findNaturalLoops().find(_.last == lastEle - 1)
        if (loopOption.isDefined && loopOption.get.head < branchingSite) {
            return true
        }

        val indexIf = cfg.bb(lastEle) match {
            case bb: BasicBlock ⇒
                val ifPos = bb.startPC.to(bb.endPC).filter(
                    cfg.code.instructions(_).isInstanceOf[If[V]]
                )
                if (ifPos.nonEmpty && !isHeadOfLoop(ifPos.head, cfg.findNaturalLoops(), cfg)) {
                    ifPos.head
                } else {
                    -1
                }
            case _ ⇒ -1
        }

        if (indexIf != -1) {
            // For else-if constructs
            isCondWithoutElse(indexIf, cfg, processedIfs)
        } else {
            // For every successor (except the very last one), execute a DFS to check whether the
            // very last element is a successor. If so, this represents a path past the if (or
            // if-elseif).
            var reachableCount = successors.count(_ == lastEle)
            successors.foreach { next ⇒
                val seenNodes = ListBuffer[CFGNode](cfg.bb(branchingSite), cfg.bb(next))
                val toVisitStack = mutable.Stack[CFGNode](cfg.bb(next).successors.toArray: _*)
                while (toVisitStack.nonEmpty) {
                    val from = toVisitStack.pop()
                    val to = from.successors
                    if ((from.nodeId == lastEle || to.contains(cfg.bb(lastEle))) &&
                        from.nodeId >= branchingSite) {
                        reachableCount += 1
                    }
                    seenNodes.append(from)
                    toVisitStack.pushAll(to.filter(!seenNodes.contains(_)))
                }
            }
            if (reachableCount > 1) {
                true
            } else {
                processedIfs(branchingSite) = Unit
                false
            }
        }
    }

    /**
     * Based on the member `cfg` of this instance, this function checks whether a path from node
     * `from` to node `to` exists. If so, `true` is returned and `false otherwise`. Optionally, a
     * list of `alreadySeen` elements can be passed which influences which paths are to be followed
     * (when assembling a path ''p'' and the next node, ''n_p'' in ''p'', is a node that was already
     * seen, the path will not be continued in the direction of ''n_p'' (but in other directions
     * that are not in `alreadySeen`)).
     *
     * @note This function assumes that `from` >= 0!
     */
    protected def doesPathExistTo(
        from: Int, to: Int, alreadySeen: List[Int] = List()
    ): Boolean = {
        val stack = mutable.Stack(from)
        val seenNodes = mutable.Map[Int, Unit]()
        alreadySeen.foreach(seenNodes(_)= Unit)
        seenNodes(from) = Unit

        while (stack.nonEmpty) {
            val popped = stack.pop()
            cfg.bb(popped).successors.foreach { nextBlock ⇒
                // -1 is okay, as this value will not be processed (due to the flag processBlock)
                var startPC = -1
                var endPC = -1
                var processBlock = true
                nextBlock match {
                    case bb: BasicBlock ⇒
                        startPC = bb.startPC; endPC = bb.endPC
                    case cn: CatchNode ⇒
                        startPC = cn.startPC; endPC = cn.endPC
                    case _ ⇒ processBlock = false
                }

                if (processBlock) {
                    if (startPC >= to && endPC <= to) {
                        // When the `to` node was seen, immediately return
                        return true
                    } else if (!seenNodes.contains(startPC)) {
                        stack.push(startPC)
                        seenNodes(startPC) = Unit
                    }
                }
            }
        }

        // When this part is reached, no path could be found
        false
    }

    /**
     * Determines the bounds of a loop, that is the indices of the first and the last statement.
     *
     * @param index The index of the statement that is the `if` statement of the loop. This function
     *              can deal with `if`s within the loop header or loop footer.
     * @return Returns the index of the very first statement of the loop as well as the index of the
     *         very last statement index.
     */
    private def getStartAndEndIndexOfLoop(index: Int): (Int, Int) = {
        var startIndex = -1
        var endIndex = -1
        val relevantLoop = cfg.findNaturalLoops().filter(nextLoop ⇒
            // The given index might belong either to the start or to the end of a loop
            isHeadOfLoop(index, List(nextLoop), cfg) || isEndOfLoop(index, List(nextLoop)))
        if (relevantLoop.nonEmpty) {
            startIndex = relevantLoop.head.head
            endIndex = relevantLoop.head.last
        }
        (startIndex, endIndex)
    }

    /**
     * This function determines the type of the [[If]] statement, i.e., an element of
     * [[NestedPathType]] as well as the indices of the very first and very last statement that
     * belong to the `if`.
     *
     * @param stmt The index of the statement to process. This statement must be of type [[If]].
     * @param processedIfs A map that serves as a look-up table to 1) determine which `if`s have
     *                     already been processed (and thus will not be processed again), and 2) to
     *                     extend this table by the `if`s encountered in this procedure.
     * @return Returns the start index, end index, and type of the `if` in that order.
     *
     * @note For further details, see [[getStartAndEndIndexOfCondWithAlternative]],
     *       [[getStartAndEndIndexOfCondWithoutAlternative]], and [[determineTryCatchBounds]].
     */
    protected def processIf(
        stmt: Int, processedIfs: mutable.Map[Int, Unit.type]
    ): CSInfo = {
        val csType = determineTypeOfIf(stmt, processedIfs)
        val (startIndex, endIndex) = csType match {
            case NestedPathType.Repetition ⇒
                processedIfs(stmt) = Unit
                getStartAndEndIndexOfLoop(stmt)
            case NestedPathType.CondWithoutAlternative ⇒
                getStartAndEndIndexOfCondWithoutAlternative(stmt, processedIfs)
            // _ covers CondWithAlternative and TryCatchFinally, however, the latter one should
            // never be present as the element referring to stmts is / should be an If
            case _ ⇒
                getStartAndEndIndexOfCondWithAlternative(stmt, processedIfs)
        }
        (startIndex, endIndex, csType)
    }

    /**
     * This function determines the indices of the very first and very last statement that belong to
     * the `switch` statement as well as the type of the `switch` (
     * [[NestedPathType.CondWithAlternative]] if the `switch` has a `default` case and
     * [[NestedPathType.CondWithoutAlternative]] otherwise.
     *
     * @param stmt The index of the statement to process. This statement must be of type [[Switch]].
     *
     * @return Returns the start index, end index, and type of the `switch` in that order.
     */
    protected def processSwitch(stmt: Int): CSInfo = {
        val switch = cfg.code.instructions(stmt).asSwitch
        val caseStmts = switch.caseStmts.sorted
        // From the last to the first one, find the first case that points after the switch
        val caseGotoOption = caseStmts.reverse.find { caseIndex ⇒
            cfg.code.instructions(caseIndex - 1).isInstanceOf[Goto]
        }
        // If no such case is present, find the next goto after the default case
        val posGoTo = if (caseGotoOption.isEmpty) {
            var i = switch.defaultStmt
            while (!cfg.code.instructions(i).isInstanceOf[Goto]) {
                i += 1
            }
            i
        } else caseGotoOption.get - 1
        var end = cfg.code.instructions(posGoTo).asGoto.targetStmt - 1
        // In case the goto points at the a loop, do not set the start index of the loop as end
        // position but the index of the goto
        if (end < stmt) {
            end = posGoTo
        }

        val containsDefault = caseStmts.length == caseStmts.distinct.length
        val pathType = if (containsDefault) NestedPathType.CondWithAlternative else
            NestedPathType.CondWithoutAlternative

        (stmt, end, pathType)
    }

    /**
     * @param stmtIndex The index of the instruction that is an [[If]] and for which the type is to
     *                  be determined.
     * @return Returns a value in [[NestedPathType.Value]] except
     *         [[NestedPathType.TryCatchFinally]] (as their construction does not involve an [[If]]
     *         statement).
     */
    protected def determineTypeOfIf(
        stmtIndex: Int, processedIfs: mutable.Map[Int, Unit.type]
    ): NestedPathType.Value = {
        // Is the first condition enough to identify loops?
        val loops = cfg.findNaturalLoops()
        // The if might belong to the head or end of the loop
        if (isHeadOfLoop(stmtIndex, loops, cfg) || isEndOfLoop(stmtIndex, loops)) {
            NestedPathType.Repetition
        } else if (isCondWithoutElse(stmtIndex, cfg, processedIfs)) {
            NestedPathType.CondWithoutAlternative
        } else {
            NestedPathType.CondWithAlternative
        }
    }

    /**
     * Finds all control structures within [[cfg]]. This includes `try-catch`.
     * `try-catch` blocks will be treated specially in the sense that, if a ''finally'' block
     * exists, it will not be included in the path from ''start index'' to ''destination index''
     * (however, as ''start index'' marks the beginning of the `try-catch` and ''destination index''
     * everything up to the ''finally block'', ''finally'' statements after the exception handling
     * will be included and need to be filtered out later.
     *
     * @return Returns all found control structures in a flat structure; for the return format, see
     *         [[CSInfo]]. The elements are returned in a sorted by ascending start index.
     */
    protected def findControlStructures(startSites: List[Int], endSite: Int): List[CSInfo] = {
        // foundCS stores all found control structures as a triple in the form (start, end, type)
        var foundCS = ListBuffer[CSInfo]()
        // For a fast loop-up which if statements have already been processed
        val processedIfs = mutable.Map[Int, Unit.type]()
        val processedSwitches = mutable.Map[Int, Unit.type]()
        val stack = mutable.Stack[CFGNode]()
        val seenCFGNodes = mutable.Map[CFGNode, Unit.type]()

        startSites.reverse.foreach { site ⇒
            stack.push(cfg.bb(site))
            seenCFGNodes(cfg.bb(site)) = Unit
        }

        while (stack.nonEmpty) {
            val next = stack.pop()
            seenCFGNodes(next) = Unit

            next match {
                case bb: BasicBlock ⇒
                    for (i ← bb.startPC.to(bb.endPC)) {
                        cfg.code.instructions(i) match {
                            case _: If[V] if !processedIfs.contains(i) ⇒
                                foundCS.append(processIf(i, processedIfs))
                                processedIfs(i) = Unit
                            case _: Switch[V] if !processedSwitches.contains(i) ⇒
                                foundCS.append(processSwitch(i))
                                processedSwitches(i) = Unit
                            case _ ⇒
                        }
                    }
                case _ ⇒
            }

            if (next.nodeId == endSite) {
                val doesPathExist = stack.filter(_.nodeId >= 0).foldLeft(false) {
                    (doesExist: Boolean, next: CFGNode) ⇒
                        doesExist || doesPathExistTo(next.nodeId, endSite)
                }
                // In case no more path exists, clear the stack which (=> no more iterations)
                if (!doesPathExist) {
                    stack.clear()
                }
            } else {
                // Add unseen successors
                next.successors.filter(!seenCFGNodes.contains(_)).foreach(stack.push)
            }
        }

        // It might be that some control structures can be removed as they are not in the relevant
        // range
        foundCS = foundCS.filterNot {
            case (start, end, _) ⇒
                (startSites.forall(start > _) && endSite < start) ||
                    (startSites.forall(_ < start) && startSites.forall(_ > end))
        }

        // Add try-catch (only those that are relevant for the given start and end sites)
        // information
        var relevantTryCatchBlocks = determineTryCatchBounds()
        // Filter out all blocks that completely surround the given start and end sites
        relevantTryCatchBlocks = relevantTryCatchBlocks.filter {
            case (tryStart, tryEnd, _) ⇒
                val tryCatchParts = buildTryCatchPath(tryStart, tryEnd, fill = false)
                !tryCatchParts._2.exists {
                    case (nextInnerStart, nextInnerEnd) ⇒
                        startSites.forall(_ >= nextInnerStart) && endSite <= nextInnerEnd
                }
        }
        // Keep the try-catch blocks that are (partially) within the start and end sites
        relevantTryCatchBlocks = relevantTryCatchBlocks.filter {
            case (tryStart, _, _) ⇒
                startSites.exists(tryStart >= _) && tryStart <= endSite
        }

        foundCS.appendAll(relevantTryCatchBlocks)
        foundCS.sortBy { case (start, _, _) ⇒ start }.toList
    }

    /**
     * This function serves as a wrapper function for unified processing of different elements,
     * i.e., different types of [[CSInfo]] that are stored in `toTransform`.
     * For further information, see [[buildRepetitionPath]], [[buildCondPath]],
     * [[buildPathForSwitch]], and [[buildTryCatchPath]].
     */
    protected def buildPathForElement(
        toTransform: HierarchicalCSOrder, fill: Boolean
    ): (Path, List[(Int, Int)]) = {
        val element = toTransform.hierarchy.head._1.get
        val start = element._1
        val end = element._2
        if (cfg.code.instructions(start).isInstanceOf[Switch[V]]) {
            buildPathForSwitch(start, end, fill)
        } else {
            element._3 match {
                case NestedPathType.Repetition ⇒
                    buildRepetitionPath(start, end, fill)
                case NestedPathType.CondWithAlternative ⇒
                    buildCondPath(start, end, NestedPathType.CondWithAlternative, fill)
                case NestedPathType.CondWithoutAlternative ⇒
                    buildCondPath(start, end, NestedPathType.CondWithoutAlternative, fill)
                case NestedPathType.TryCatchFinally ⇒
                    buildTryCatchPath(start, end, fill)
            }
        }
    }

    /**
     * This function takes a flat list of control structure information and transforms it into a
     * hierarchical order.
     *
     * @param cs A list of control structure elements that are to be transformed into a hierarchical
     *           representation. This function assumes, that the control structures are sorted by
     *           start index in ascending order.
     * @return The hierarchical structure.
     *
     * @note This function assumes that `cs` contains at least one element!
     */
    protected def hierarchicallyOrderControlStructures(cs: List[CSInfo]): HierarchicalCSOrder = {
        // childrenOf stores seen control structures in the form: parent, children. Note that for
        // performance reasons (see foreach loop below), the elements are inserted in reversed order
        // in terms of the `cs` order for less loop iterations in the next foreach loop
        val childrenOf = mutable.ListBuffer[(CSInfo, ListBuffer[CSInfo])]()
        childrenOf.append((cs.head, ListBuffer()))

        // Stores as key a CS and as value the parent element (if an element, e, is not contained in
        // parentOf, e does not have a parent
        val parentOf = mutable.Map[CSInfo, CSInfo]()
        // Find the direct parent of each element (if it exists at all)
        cs.tail.foreach { nextCS ⇒
            var nextPossibleParentIndex = 0
            var parent: Option[Int] = None
            // Use a while instead of a foreach loop in order to stop when the parent was found
            while (parent.isEmpty && nextPossibleParentIndex < childrenOf.length) {
                val possibleParent = childrenOf(nextPossibleParentIndex)
                // The parent element must contain the child
                if (nextCS._1 > possibleParent._1._1 && nextCS._1 <= possibleParent._1._2) {
                    parent = Some(nextPossibleParentIndex)
                } else {
                    nextPossibleParentIndex += 1
                }
            }
            if (parent.isDefined) {
                childrenOf(parent.get)._2.append(nextCS)
                parentOf(nextCS) = childrenOf(parent.get)._1
            }
            childrenOf.prepend((nextCS, ListBuffer()))
        }

        // Convert to a map for faster accesses in the following part
        val mapChildrenOf = mutable.Map[CSInfo, ListBuffer[CSInfo]]()
        childrenOf.foreach { nextCS ⇒ mapChildrenOf(nextCS._1) = nextCS._2 }

        HierarchicalCSOrder(List((
            None, cs.filter(!parentOf.contains(_)).map(buildHierarchy(_, mapChildrenOf))
        )))
    }

    /**
     * This function transforms a hierarchy into a [[Path]].
     *
     * @param topElements A list of the elements which are present on the top-most level in the
     *                    hierarchy.
     * @param startIndex `startIndex` serves as a way to build a path between the first statement
     *                   (which is not necessarily a control structure) and the very first control
     *                  structure. For example, assume that the first control structure begins at
     *                   statement 5. `startIndex` will then be used to fill the gap `startIndex`
     *                   and 5.
     * @param endIndex  `endIndex` serves as a way to build a path between the last statement of a
     *                control structure (which is not necessarily the end of a scope of interest,
     *                such as a method) and the last statement (e.g., in `cfg`).
     * @return Returns the transformed [[Path]].
     */
    protected def hierarchyToPath(
        topElements: List[HierarchicalCSOrder], startIndex: Int, endIndex: Int
    ): Path = {
        val finalPath = ListBuffer[SubPath]()
        // For the outer-most call, this is not the start index of the last control structure but of
        // the start PC of the first basic block
        var indexLastCSEnd = startIndex

        // Recursively transform the hierarchies to paths
        topElements.foreach { nextTopEle ⇒
            // Build path up to the next control structure
            val nextCSStart = nextTopEle.hierarchy.head._1.get._1
            indexLastCSEnd.until(nextCSStart).foreach { i ⇒
                finalPath.append(FlatPathElement(i))
            }

            val children = nextTopEle.hierarchy.head._2
            if (children.isEmpty) {
                // Recursion anchor: Build path for the correct type
                val (subpath, _) = buildPathForElement(nextTopEle, fill = true)
                // Control structures consist of only one element (NestedPathElement), thus "head"
                // is enough
                finalPath.append(subpath.elements.head)
            } else {
                val startIndex = nextTopEle.hierarchy.head._1.get._1
                val endIndex = nextTopEle.hierarchy.head._1.get._2
                val childrenPath = hierarchyToPath(children, startIndex, endIndex)
                var insertIndex = 0
                val (subpath, startEndPairs) = buildPathForElement(nextTopEle, fill = false)
                // npe is the nested path element that was produced above (head is enough as this
                // list will always contain only one element, due to fill=false)
                val npe = subpath.elements.head.asInstanceOf[NestedPathElement]
                val isRepElement = npe.elementType.getOrElse(NestedPathType.TryCatchFinally) ==
                    NestedPathType.Repetition
                var lastInsertedIndex = 0
                childrenPath.elements.foreach { nextEle ⇒
                    if (isRepElement) {
                        npe.element.append(nextEle)
                    } else {
                        if (insertIndex < npe.element.length) {
                            npe.element(insertIndex).asInstanceOf[NestedPathElement].element.append(
                                nextEle
                            )
                        }
                    }

                    lastInsertedIndex = nextEle match {
                        case fpe: FlatPathElement     ⇒ fpe.element
                        case inner: NestedPathElement ⇒ Path.getLastElementInNPE(inner).element
                        // Compiler wants it but should never be the case!
                        case _                        ⇒ -1
                    }
                    if (insertIndex < startEndPairs.length &&
                        lastInsertedIndex >= startEndPairs(insertIndex)._2) {
                        insertIndex += 1
                    }
                }
                // Fill the current NPE if necessary
                val currentToInsert = ListBuffer[FlatPathElement]()
                if (insertIndex < startEndPairs.length) {
                    currentToInsert.appendAll((lastInsertedIndex + 1).to(
                        startEndPairs(insertIndex)._2
                    ).map(FlatPathElement))
                    if (isRepElement) {
                        npe.element.appendAll(currentToInsert)
                    } else {
                        var insertPos = npe.element(insertIndex).asInstanceOf[NestedPathElement]
                        insertPos.element.appendAll(currentToInsert)
                        insertIndex += 1
                        // Fill the rest NPEs if necessary
                        insertIndex.until(startEndPairs.length).foreach { i ⇒
                            insertPos = npe.element(i).asInstanceOf[NestedPathElement]
                            insertPos.element.appendAll(
                                startEndPairs(i)._1.to(startEndPairs(i)._2).map(FlatPathElement)
                            )
                        }
                    }
                }
                // Make sure to have no empty lists
                val subPathNpe = subpath.elements.head.asInstanceOf[NestedPathElement]
                val subPathToAdd = NestedPathElement(
                    subPathNpe.element.filter {
                        case npe: NestedPathElement ⇒ npe.element.nonEmpty
                        case _                      ⇒ true
                    }, subPathNpe.elementType
                )
                finalPath.append(subPathToAdd)
            }
            indexLastCSEnd = nextTopEle.hierarchy.head._1.get._2 + 1
        }

        finalPath.appendAll(indexLastCSEnd.to(endIndex).map(FlatPathElement))
        Path(finalPath.toList)
    }

    /**
     * Implementations of this function find all paths starting from the sites, given by
     * `startSites`, within the provided control flow graph, `cfg`. As this is executed within the
     * context of a string definition analysis, implementations are free to decide whether they
     * include only statements that work on [[StringBuffer]] / [[StringBuilder]] or include all
     * statements in the paths.
     *
     * @param startSites A list of possible start sites, that is, initializations. Several start
     *                   sites denote that an object is initialized within a conditional.
     *                   Implementations may or may not use this list (however, they should indicate
     *                   whether it is required or not).
     * @param endSite    An end site, that is, if the element corresponding to `endSite` is
     *                   encountered, the finding procedure can be early stopped. Implementations
     *                   may or may not use this list (however, they should indicate whether it is
     *                   required or not).
     * @return Returns all found paths as a [[Path]] object. That means, the return object is a flat
     *         structure, however, captures all hierarchies and (nested) flows. Note that a
     *         [[NestedPathElement]] with only one child can either refer to a loop or an ''if''
     *         that has no ''else'' block (from a high-level perspective). It is the job of the
     *         implementations to attach these information to [[NestedPathElement]]s (so that
     *         procedures using results of this function do not need to re-process).
     */
    def findPaths(startSites: List[Int], endSite: Int): Path

}
