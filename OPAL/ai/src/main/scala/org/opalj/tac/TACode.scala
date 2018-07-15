/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.Attribute
import org.opalj.br.ExceptionHandlers
import org.opalj.br.LineNumberTable
import org.opalj.br.SimilarityTestConfiguration
import org.opalj.br.CodeSequence
import org.opalj.br.cfg.CFG

/**
 * Contains the 3-address code of a method.
 *
 * == Attributes ==
 * The following code attributes are `directly` reused (i.e., the PCs are not transformed):
 * - LineNumberTableAttribute; the statements keep the reference to the underlying/original
 *   instruction which is used to retrieve the respective information.
 *
 * @param params The variables which store the method's explicit and implicit (`this` in case
 *               of an instance method) parameters.
 *               In case of the ai-based representation (TACAI - default representation),
 *               the variables are returned which store (the initial) parameters. If these variables
 *               are written and we have a loop which includes the very first instruction, the
 *               value will reflect this usage.
 *               In case of the naive representation it "just" contains the names of the
 *               registers which store the parameters.
 * @param pcToIndex The mapping between the pcs of the original bytecode instructions to the
 *               index of the first statement that was generated for the bytecode instruction
 *               - if any. For details see `TACNaive` and `TACAI`
 *
 * @author Michael Eichberg
 */
case class TACode[P <: AnyRef, +V <: Var[V]](
        params:            Parameters[P],
        stmts:             Array[Stmt[V]], // CONST
        pcToIndex:         Array[Int],
        cfg:               CFG[Stmt[V], TACStmts[V]],
        exceptionHandlers: ExceptionHandlers,
        lineNumberTable:   Option[LineNumberTable]
// TODO Support the rewriting of TypeAnnotations etc.
) extends Attribute with CodeSequence[Stmt[V]] {

    final override def instructions: Array[Stmt[V]] = stmts

    final override def pcOfPreviousInstruction(pc: Int): Int = {
        // The representation is compact: hence, the previous instruction/statement just
        // has the current index/pc - 1.
        pc - 1
    }

    final override def pcOfNextInstruction(pc: Int): Int = pc + 1

    override def kindId: Int = TACode.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this equals other
    }

    def firstLineNumber: Option[Int] = lineNumberTable.flatMap(_.firstLineNumber()) // IMPROVE [L2] Use IntOption

    def lineNumber(index: Int): Option[Int] = { // IMPROVE [L2] Use IntOption
        lineNumberTable.flatMap(_.lookupLineNumber(stmts(index).pc))
    }

    override def toString: String = {
        val txtParams = s"params=($params)"
        val stmtsWithIndex = stmts.iterator.zipWithIndex.map { e ⇒ val (s, i) = e; s"$i: $s" }
        val txtStmts = stmtsWithIndex.mkString("stmts=(\n\t", ",\n\t", "\n)")
        val txtExceptionHandlers =
            if (exceptionHandlers.nonEmpty)
                exceptionHandlers.mkString(",exceptionHandlers=(\n\t", ",\n\t", "\n)")
            else
                ""
        val txtLineNumbers =
            lineNumberTable match {
                case Some(lnt) ⇒ lnt.lineNumbers.mkString(",lineNumberTable=(\n\t", ",\n\t", "\n)")
                case None      ⇒ ""
            }
        s"TACode($txtParams,$txtStmts,cfg=$cfg$txtExceptionHandlers$txtLineNumbers)"
    }

}
object TACode {

    final val KindId = 1003

}
