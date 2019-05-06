/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.util.{Arrays ⇒ JArrays}

import org.opalj.value.ValueInformation
import org.opalj.br.Attribute
import org.opalj.br.CodeSequence
import org.opalj.br.ExceptionHandlers
import org.opalj.br.SimilarityTestConfiguration
import org.opalj.br.cfg.CFG
import org.opalj.br.Code

/**
 * Contains the 3-address code of a method.
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
 * @tparam V     The type of Vars used by the underlying code.
 *               Given that the stmts array is conceptually immutable - i.e., no client is allowed
 *               to change it(!) - the type V is actually co-variant, but we cannot express this.
 *
 * @author Michael Eichberg
 */
final class TACode[P <: AnyRef, V <: Var[V]](
        val params:            Parameters[P],
        val stmts:             Array[Stmt[V]], // IMPROVE use ConstCovariantArray to make it possible to make V covariant!
        val pcToIndex:         Array[Int],
        val cfg:               CFG[Stmt[V], TACStmts[V]],
        val exceptionHandlers: ExceptionHandlers
) extends Attribute with CodeSequence[Stmt[V]] {

    override def instructions: Array[Stmt[V]] = stmts

    override def pcOfPreviousInstruction(pc: Int): Int = {
        // The representation is compact: hence, the previous instruction/statement just
        // has the current index/pc - 1.
        pc - 1
    }

    override def pcOfNextInstruction(pc: Int): Int = pc + 1

    override def kindId: Int = TACode.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this equals other
    }

    def firstLineNumber(code: Code): Option[Int] = {
        code.lineNumberTable.flatMap(_.firstLineNumber()) // IMPROVE [L2] Use IntOption
    }

    def lineNumber(code: Code, index: Int): Option[Int] = { // IMPROVE [L2] Use IntOption
        code.lineNumberTable.flatMap(_.lookupLineNumber(stmts(index).pc))
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: TACode[_, _] ⇒
                // Recall that the CFG is derived from the stmts and therefore necessarily
                // equal when the statements are equal.
                val thisStmts = this.stmts.asInstanceOf[Array[AnyRef]]
                val thatStmts = that.stmts.asInstanceOf[Array[AnyRef]]
                JArrays.equals(thisStmts, thatStmts) &&
                    this.params == that.params &&
                    JArrays.equals(this.pcToIndex, that.pcToIndex) &&
                    this.exceptionHandlers == that.exceptionHandlers

            case _ ⇒ false
        }
    }

    override lazy val hashCode: Int = {
        // In the following we do not consider the CFG as it is "just" a derived
        // data structure.
        ((params.hashCode * 31 +
            JArrays.hashCode(stmts.asInstanceOf[Array[AnyRef]])) * 31 +
            JArrays.hashCode(pcToIndex)) * 31 +
            exceptionHandlers.hashCode * 31
    }

    /** Detaches the 3-address code from the underlying abstract interpreation result. */
    def detach(implicit ev: V <:< DUVar[ValueInformation]): TACode[P, DUVar[ValueInformation]] = {
        new TACode(
            params,
            this.stmts.map(_.toCanonicalForm),
            pcToIndex,
            cfg.asInstanceOf[CFG[Stmt[DUVar[ValueInformation]], TACStmts[DUVar[ValueInformation]]]],
            exceptionHandlers
        )
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
        s"TACode($txtParams,$txtStmts,cfg=$cfg$txtExceptionHandlers)"
    }

}
object TACode {

    final val KindId = 1003

    def apply[P <: AnyRef, V <: Var[V]](
        params:            Parameters[P],
        stmts:             Array[Stmt[V]],
        pcToIndex:         Array[Int],
        cfg:               CFG[Stmt[V], TACStmts[V]],
        exceptionHandlers: ExceptionHandlers
    ): TACode[P, V] = {
        new TACode(params, stmts, pcToIndex, cfg, exceptionHandlers)
    }

    def unapply[P <: AnyRef, V <: Var[V]](
        code: TACode[P, V]
    ): Some[(Parameters[P], Array[Stmt[V]], Array[Int], CFG[Stmt[V], TACStmts[V]], ExceptionHandlers)] = {
        Some((
            code.params,
            code.stmts,
            code.pcToIndex,
            code.cfg,
            code.exceptionHandlers
        ))
    }

}
