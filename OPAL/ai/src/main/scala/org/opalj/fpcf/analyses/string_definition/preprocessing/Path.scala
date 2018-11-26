/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.preprocessing

import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.New
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualFunctionCall
import org.opalj.value.ValueInformation

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * @author Patrick Mell
 */

/**
 * [[SubPath]] represents the general item that forms a [[Path]].
 */
sealed class SubPath()

/**
 * A flat element, e.g., for representing a single statement. The statement is identified by
 * `element`.
 */
case class FlatPathElement(element: Int) extends SubPath

/**
 * Identifies the nature of a nested path element.
 */
object NestedPathType extends Enumeration {

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

}

/**
 * A nested path element, that is, items can be used to form arbitrary structures / hierarchies.
 * `element` holds all child elements. Path finders should set the `elementType` property whenever
 * possible, i.e., when they compute / have this information.
 */
case class NestedPathElement(
        element:     ListBuffer[SubPath],
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
     * definitions and usages of `obj`within `stmts`. These sites are then returned in a single
     * sorted list.
     */
    private def getAllDefAndUseSites(
        obj: DUVar[ValueInformation], stmts: Array[Stmt[V]]
    ): List[Int] = {
        val defAndUses = ListBuffer[Int]()
        val stack = mutable.Stack[Int](obj.definedBy.toArray: _*)

        while (stack.nonEmpty) {
            val popped = stack.pop()
            if (!defAndUses.contains(popped)) {
                defAndUses.append(popped)

                stmts(popped) match {
                    case a: Assignment[V] if a.expr.isInstanceOf[VirtualFunctionCall[V]] ⇒
                        stack.pushAll(a.expr.asVirtualFunctionCall.receiver.asVar.definedBy.toArray)
                    case a: Assignment[V] if a.expr.isInstanceOf[New] ⇒
                        stack.pushAll(a.targetVar.usedBy.toArray)
                    case _ ⇒
                }
            }
        }

        defAndUses.toList
    }

    /**
     * Accumulator function for transforming a path into its lean equivalent. This function turns
     * [[NestedPathElement]]s into lean [[NestedPathElement]]s. In case a (sub) path is empty,
     * `None` is returned and otherwise the lean (sub) path.
     */
    private def makeLeanPathAcc(
        toProcess: NestedPathElement, siteMap: Map[Int, Unit.type]
    ): Option[NestedPathElement] = {
        val elements = ListBuffer[SubPath]()

        toProcess.element.foreach {
            case fpe: FlatPathElement ⇒
                if (siteMap.contains(fpe.element)) {
                    elements.append(fpe.copy())
                }
            case npe: NestedPathElement ⇒
                val nested = makeLeanPathAcc(npe, siteMap)
                if (nested.isDefined) {
                    elements.append(nested.get)
                }
            // For the case the element is a SubPath (should never happen but the compiler want it)
            case _ ⇒
        }

        if (elements.nonEmpty) {
            Some(NestedPathElement(elements, toProcess.elementType))
        } else {
            None
        }
    }

    /**
     * Takes `this` path and transforms it into a new [[Path]] where only those sites are contained
     * that either use or define `obj`.
     *
     * @param obj Identifies the object of interest. That is, all definition and use sites of this
     *            object will be kept in the resulting lean path. `obj` should refer to a use site,
     *            most likely corresponding to an (implicit) `toString` call.
     * @param stmts A list of look-up statements, i.e., a program / method description in which
     *              `obj` occurs.
     * @return Returns a lean path of `this` path. That means, `this` instance will be stripped to
     *         contain only [[FlatPathElement]]s and [[NestedPathElement]]s that contain a
     *         definition or usage of `obj`. This includes the removal of [[NestedPathElement]]s
     *         not containing `obj`. In case `this` path does not contain `obj` at all, `None` will
     *         be returned.
     *
     * @note This function does not change the underlying `this` instance. Furthermore, all relevant
     *       elements for the lean path will be copied, i.e., `this` instance and the returned
     *       instance do not share any references.
     */
    def makeLeanPath(obj: DUVar[ValueInformation], stmts: Array[Stmt[V]]): Option[Path] = {
        // Transform the list into a map to have a constant access time
        val siteMap = Map(getAllDefAndUseSites(obj, stmts) map { s ⇒ (s, Unit) }: _*)
        val leanPath = ListBuffer[SubPath]()
        elements.foreach {
            case fpe: FlatPathElement if siteMap.contains(fpe.element) ⇒
                leanPath.append(fpe)
            case npe: NestedPathElement ⇒
                val leanedPath = makeLeanPathAcc(npe, siteMap)
                if (leanedPath.isDefined) {
                    leanPath.append(leanedPath.get)
                }
            case _ ⇒
        }

        if (elements.isEmpty) {
            None
        } else {
            Some(Path(leanPath.toList))
        }
    }

}
