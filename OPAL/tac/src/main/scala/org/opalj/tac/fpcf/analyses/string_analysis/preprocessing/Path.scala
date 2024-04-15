/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.value.ValueInformation

/**
 * @author Maximilian Rüsch
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
}

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
     * Takes `this` path and transforms it into a new [[Path]] where only those sites are contained that either use or
     * define `obj`.
     *
     * @param obj Identifies the object of interest. That is, all definition and use sites of this object will be kept
     *            in the resulting lean path. `obj` should refer to a use site, most likely corresponding to an
     *            (implicit) `toString` call.
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
            case fpe: FlatPathElement if pcMap.contains(fpe.pc) && fpe.stmtIndex <= endSite => leanPath.append(fpe)
            case _                                                                          =>
        }

        Path(leanPath.toList)
    }
}
