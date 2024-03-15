/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

import scala.annotation.tailrec

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.value.ValueInformation

/**
 * @author Patrick Mell
 */

/**
 * [[SubPath]] represents the general item that forms a [[Path]].
 */
sealed class SubPath()

/**
 * A flat element, e.g., for representing a single statement. The statement is identified with `pc` by its [[org.opalj.br.PC]].
 */
class FlatPathElement private[FlatPathElement] (val pc: Int) extends SubPath {
    def stmtIndex(implicit pcToIndex: Array[Int]): Int = valueOriginOfPC(pc, pcToIndex).get

    def copy = new FlatPathElement(pc)
}

object FlatPathElement extends SubPath {
    def apply(defSite: Int)(implicit stmts: Array[Stmt[V]]) = new FlatPathElement(pcOfDefSite(defSite))

    def unapply(fpe: FlatPathElement)(implicit pcToIndex: Array[Int]): Some[Int] = Some(fpe.stmtIndex)

    def fromPC(pc: Int) = new FlatPathElement(pc)
    def invalid = new FlatPathElement(-1)
}

/**
 * Identifies the nature of a nested path element.
 */
case object NestedPathType extends Enumeration {

    /**
     * Used to mark any sort of loops.
     */
    val Repetition: NestedPathType.Value = Value

    /**
     *  Use this type to mark a conditional that has an alternative that is guaranteed to be
     *  executed. For instance, an `if` with an `else` block would fit this type, as would a `case`
     *  with a `default`. These are just examples for high-level languages. The concepts, however,
     *  can be applied to low-level format as well.
     */
    val CondWithAlternative: NestedPathType.Value = Value

    /**
     * Use this type to mark a conditional that is not necessarily executed. For instance, an `if`
     * without an `else` (but possibly several `else if` fits this category. Again, this is to be
     * mapped to low-level representations as well.
     */
    val CondWithoutAlternative: NestedPathType.Value = Value

    /**
     * Use this type to mark a switch that does not contain a `default` statement.
     */
    val SwitchWithoutDefault: NestedPathType.Value = Value

    /**
     * Use this type to mark a switch that contains a `default` statement.
     */
    val SwitchWithDefault: NestedPathType.Value = Value

    /**
     * This type is to mark `try-catch` or `try-catch-finally` constructs.
     */
    val TryCatchFinally: NestedPathType.Value = Value
}

/**
 * A nested path element, that is, items can be used to form arbitrary structures / hierarchies.
 * `element` holds all child elements. Path finders should set the `elementType` property whenever
 * possible, i.e., when they compute / have this information.
 */
case class NestedPathElement(
    element:     Seq[SubPath],
    elementType: Option[NestedPathType.Value]
) extends SubPath

/**
 * Models a path by assembling it out of [[SubPath]] elements.
 *
 * @param elements The elements that belong to a path.
 */
case class Path(elements: List[SubPath]) {

    /**
     * Takes an object of interest, `obj`, and a list of statements, `stmts` and finds all
     * definitions and usages of `obj` within `stmts`. These sites are then returned in a single
     * sorted list.
     */
    private def getAllDefAndUseSites(obj: DUVar[ValueInformation], stmts: Array[Stmt[V]]): List[Int] = {
        val defAndUses = ListBuffer[Int]()
        val stack = mutable.Stack[Int](obj.definedBy.toList: _*)

        while (stack.nonEmpty) {
            val nextDefUseSite = stack.pop()
            if (!defAndUses.contains(nextDefUseSite)) {
                defAndUses.append(nextDefUseSite)

                stmts(nextDefUseSite) match {
                    case a: Assignment[V] if a.expr.isInstanceOf[VirtualFunctionCall[V]] =>
                        val receiver = a.expr.asVirtualFunctionCall.receiver.asVar
                        stack.pushAll(receiver.asVar.definedBy.filter(_ >= 0).toArray)
                        // TODO: Does the following line add too much (in some cases)???
                        stack.pushAll(a.targetVar.asVar.usedBy.toArray)
                    case a: Assignment[V] if a.expr.isInstanceOf[New] =>
                        stack.pushAll(a.targetVar.usedBy.toArray)
                    case _ =>
                }
            }
        }

        defAndUses.toList.sorted
    }

    /**
     * Checks whether a [[FlatPathElement]] with the given `element` is contained in the given `subpath`. This function
     * does a deep search, i.e., will also find the element if it is contained within [[NestedPathElement]]s.
     */
    private def containsPathElementWithPC(subpath: NestedPathElement, pc: Int): Boolean = {
        subpath.element.foldLeft(false) { (old: Boolean, nextSubpath: SubPath) =>
            old || (nextSubpath match {
                case fpe: FlatPathElement   => fpe.pc == pc
                case npe: NestedPathElement => containsPathElementWithPC(npe, pc)
                case e                      => throw new IllegalStateException(s"Unexpected path element $e")
            })
        }
    }

    /**
     * Takes a [[NestedPathElement]] and removes the outermost nesting, i.e., the path contained
     * in `npe` will be the path being returned.
     */
    @tailrec private def removeOuterBranching(npe: NestedPathElement): Seq[SubPath] = {
        if (npe.element.tail.isEmpty) {
            npe.element.head match {
                case innerNpe: NestedPathElement => removeOuterBranching(innerNpe)
                case fpe: SubPath                => Seq(fpe)
            }
        } else {
            npe.element
        }
    }

    /**
     * Takes a [[NestedPathElement]], `npe`, and an `endSite` and strips all branches that do not
     * contain `endSite`. ''Stripping'' here means to clear the other branches.
     * For example, assume `npe=[ [3, 5], [7, 9] ]` and `endSite=7`, the this function will return
     * `[ [], [7, 9] ]`. This function can handle deeply nested [[NestedPathElement]] expressions as
     * well.
     */
    private def stripUnnecessaryBranches(npe: NestedPathElement, endSite: Int): NestedPathElement = {
        val strippedElements = npe.element.map {
            case innerNpe @ NestedPathElement(_, elementType) if elementType.isEmpty =>
                if (!containsPathElementWithPC(innerNpe, endSite)) {
                    NestedPathElement(Seq.empty, None)
                } else {
                    innerNpe
                }
            case innerNpe: NestedPathElement =>
                stripUnnecessaryBranches(innerNpe, endSite)
            case pe => pe
        }
        NestedPathElement(strippedElements, npe.elementType)
    }

    /**
     * Accumulator function for transforming a path into its lean equivalent. This function turns
     * [[NestedPathElement]]s into lean [[NestedPathElement]]s and is a helper function of
     * [[makeLeanPath]].
     *
     * @param toProcess The NestedPathElement to turn into its lean equivalent.
     * @param relevantPCsMap Serves as a constant time look-up table to include only pcs that are of interest, in this
     *                      case: That belong to some object.
     *
     * @return In case a (sub) path is empty, `None` is returned and otherwise the lean (sub) path.
     */
    private def makeLeanPathAcc(
        toProcess:      NestedPathElement,
        relevantPCsMap: Map[Int, Unit]
    ): Option[NestedPathElement] = {
        val elements = ListBuffer[SubPath]()

        toProcess.element.foreach {
            case fpe: FlatPathElement =>
                if (relevantPCsMap.contains(fpe.pc)) {
                    elements.append(fpe.copy)
                }
            case npe: NestedPathElement =>
                val leanedSubPath = makeLeanPathAcc(npe, relevantPCsMap)
                val keepAlternativeBranches = toProcess.elementType match {
                    case Some(NestedPathType.CondWithAlternative) |
                        Some(NestedPathType.SwitchWithDefault) |
                        Some(NestedPathType.TryCatchFinally) => true
                    case _ => false
                }
                if (leanedSubPath.isDefined) {
                    elements.append(leanedSubPath.get)
                } else if (keepAlternativeBranches) {
                    elements.append(NestedPathElement(Seq.empty, None))
                }
            case e => throw new IllegalStateException(s"Unexpected sub path element found: $e")
        }

        if (elements.nonEmpty) {
            Some(NestedPathElement(elements.toSeq, toProcess.elementType))
        } else {
            None
        }
    }

    /**
     * Takes `this` path and transforms it into a new [[Path]] where only those sites are contained that either use or
     * define `obj`.
     *
     * @param obj Identifies the object of interest. That is, all definition and use sites of this object will be kept
     *            in the resulting lean path. `obj` should refer to a use site, most likely corresponding to an
     *            (implicit) `toString` call.
     *
     * @return Returns a lean path of `this` path. That means, `this` instance will be stripped to
     *         contain only [[FlatPathElement]]s and [[NestedPathElement]]s that contain a
     *         definition or usage of `obj`. This includes the removal of [[NestedPathElement]]s
     *         not containing `obj`.
     *
     * @note This function does not change the underlying `this` instance. Furthermore, all relevant elements for the
     *       lean path will be copied, i.e., `this` instance and the returned instance do not share any references.
     */
    def makeLeanPath(obj: DUVar[ValueInformation])(implicit tac: TAC): Path = {
        implicit val stmts: Array[Stmt[V]] = tac.stmts
        implicit val pcToIndex: Array[Int] = tac.pcToIndex

        val newOfObj = InterpretationHandler.findNewOfVar(obj, stmts)
        // Transform the list of relevant pcs into a map to have a constant access time
        val defUseSites = getAllDefAndUseSites(obj, stmts)
        val pcMap = defUseSites.filter { dus =>
            stmts(dus) match {
                case Assignment(_, _, expr: VirtualFunctionCall[V]) =>
                    val news = InterpretationHandler.findNewOfVar(expr.receiver.asVar, stmts)
                    newOfObj == news || news.exists(newOfObj.contains)
                case ExprStmt(_, expr: VirtualFunctionCall[V]) =>
                    val news = InterpretationHandler.findNewOfVar(expr.receiver.asVar, stmts)
                    newOfObj == news || news.exists(newOfObj.contains)
                case _ => true
            }
        }.map { s => (pcOfDefSite(s), ()) }.toMap
        val leanPath = ListBuffer[SubPath]()
        val endSite = obj.definedBy.toArray.max

        elements.foreach {
            case fpe: FlatPathElement if pcMap.contains(fpe.pc) && fpe.stmtIndex <= endSite =>
                leanPath.append(fpe)
            case npe: NestedPathElement =>
                val leanedPath = makeLeanPathAcc(npe, pcMap)
                if (leanedPath.isDefined) {
                    leanPath.append(leanedPath.get)
                }
            case _ =>
        }

        // If everything is within a single branch of a nested path element, ignore it (it is not
        // relevant, as everything happens within that branch anyway); for loops, remove the outer
        // body in any case (as there is no alternative branch to consider) TODO check loops again what is with loops that are never executed?
        if (leanPath.tail.isEmpty) { // TODO this throws if lean path is only one element long
            leanPath.head match {
                case npe: NestedPathElement
                    if npe.elementType.get == NestedPathType.Repetition ||
                        npe.element.tail.isEmpty =>
                    leanPath.clear()
                    leanPath.appendAll(removeOuterBranching(npe))
                case _ =>
            }
        } else {
            // If the last element is a conditional, keep only the relevant branch (the other is not
            // necessary and stripping it simplifies further steps; explicitly exclude try-catch)
            leanPath.last match {
                case npe: NestedPathElement
                    if npe.elementType.isDefined &&
                        (npe.elementType.get != NestedPathType.TryCatchFinally && npe.elementType.get != NestedPathType.SwitchWithDefault) =>
                    val newLast = stripUnnecessaryBranches(npe, endSite)
                    leanPath.remove(leanPath.size - 1)
                    leanPath.append(newLast)
                case _ =>
            }
        }

        Path(leanPath.toList)
    }
}

object Path {

    /**
     * Returns the very last [[FlatPathElement]] in this path, respecting any nesting structure. If no last element
     * exists, [[FlatPathElement.invalid]] is returned.
     */
    @tailrec def getLastElementInNPE(npe: NestedPathElement): FlatPathElement = {
        npe.element.lastOption match {
            case Some(fpe: FlatPathElement) => fpe
            case Some(npe: NestedPathElement) =>
                npe.element.last match {
                    case fpe: FlatPathElement        => fpe
                    case innerNpe: NestedPathElement => getLastElementInNPE(innerNpe)
                    case _                           => FlatPathElement.invalid
                }
            case _ => FlatPathElement.invalid
        }
    }
}
