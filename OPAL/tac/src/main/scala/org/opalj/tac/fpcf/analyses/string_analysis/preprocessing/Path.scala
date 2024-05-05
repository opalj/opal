/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package preprocessing

/**
 * A flat element, e.g., for representing a single statement. The statement is identified with `pc` by its [[org.opalj.br.PC]].
 *
 * @author Maximilian Rüsch
 */
class PathElement private[PathElement] (val pc: Int) {

    override def toString: String = s"PathElement(pc=$pc)"

    def stmtIndex(implicit pcToIndex: Array[Int]): Int = valueOriginOfPC(pc, pcToIndex).get
}

object PathElement {
    def apply(defSite: Int)(implicit stmts: Array[Stmt[V]]) = new PathElement(pcOfDefSite(defSite))

    def unapply(fpe: PathElement)(implicit pcToIndex: Array[Int]): Some[Int] = Some(fpe.stmtIndex)

    def fromPC(pc: Int) = new PathElement(pc)
}

case class Path(elements: List[PathElement])
