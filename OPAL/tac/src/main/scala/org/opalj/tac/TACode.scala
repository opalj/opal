/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.util.{Arrays => JArrays}

import org.opalj.value.ValueInformation
import org.opalj.br.Attribute
import org.opalj.br.CodeSequence
import org.opalj.br.ExceptionHandlers
import org.opalj.br.SimilarityTestConfiguration
import org.opalj.br.cfg.CFG
import org.opalj.br.Code
import org.opalj.br.PC

/**
 * Contains the 3-address code (like) representation of a method.
 *
 * OPAL offers multiple 3-address code like representations. One that is a one-to-one conversion
 * of the bytecode and which does not provide higher-level information.
 * Additionally, the (Base)TACAI represtation is offered that is targeted towards static analyses.
 * The base TACAI representation does not preserve all information which would be required to
 * regenerate the original code, but which greatly facilitates static analysis by making the
 * end-to-end def-use chains directly available.
 *
 * @tparam V     The type of Vars used by the underlying code.
 *               Given that the stmts array is conceptually immutable - i.e., no client is allowed
 *               to change it(!) - the type V is actually co-variant, but we cannot express this.
 *
 * @author Michael Eichberg
 */
sealed trait TACode[P <: AnyRef, V <: Var[V]] extends Attribute with CodeSequence[Stmt[V]] {

    /**
     * The variables which store the method's explicit and implicit (`this` in case
     * of an instance method) parameters.
     * In case of the ai-based representation (TACAI - default representation),
     * the variables are returned which store (the initial) parameters. If these variables
     * are written and we have a loop which includes the very first instruction, the
     * value will reflect this usage.
     * In case of the naive representation it "just" contains the names of the
     * registers which store the parameters.
     */
    def params: Parameters[P]

    def stmts: Array[Stmt[V]]
    /**
     * The mapping between the pcs of the original bytecode instructions to the
     * index of the first statement that was generated for the bytecode instruction -
     * if any. For details see `TACNaive` and `TACAI`.
     */
    def pcToIndex: Array[Int]
    def cfg: CFG[Stmt[V], TACStmts[V]]
    def exceptionHandlers: ExceptionHandlers

    override def instructions: Array[Stmt[V]] = stmts

    /**
     * Returns the pc of the previous instruction which is always the current pc - 1, because
     * the representation is compact.
     */
    override def pcOfPreviousInstruction(pc: Int): Int = pc - 1

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

    final override def equals(other: Any): Boolean = {
        other match {
            case that: TACode[_, _] =>
                // Recall that the CFG is derived from the stmts and therefore necessarily
                // equal when the statements are equal; this is true independent of the
                // concrete of 3-address code that we have!
                val thisStmts = this.stmts.asInstanceOf[Array[AnyRef]]
                val thatStmts = that.stmts.asInstanceOf[Array[AnyRef]]
                JArrays.equals(thisStmts, thatStmts) &&
                    this.params == that.params &&
                    JArrays.equals(this.pcToIndex, that.pcToIndex) &&
                    this.exceptionHandlers == that.exceptionHandlers

            case _ => false
        }
    }

    final override lazy val hashCode: Int = {
        // In the following we do not consider the CFG as it is "just" a derived
        // data structure.
        ((params.hashCode * 31 +
            JArrays.hashCode(stmts.asInstanceOf[Array[AnyRef]])) * 31 +
            JArrays.hashCode(pcToIndex)) * 31 +
            exceptionHandlers.hashCode * 31
    }

    protected[this] def toString(taCodeType: String, additionalParameters: String): String = {
        val txtParams = s"params=($params)"
        val stmtsWithIndex = stmts.iterator.zipWithIndex.map { e => val (s, i) = e; s"$i: $s" }
        val txtStmts = stmtsWithIndex.mkString("stmts=(\n\t", ",\n\t", "\n)")
        val txtExceptionHandlers =
            if (exceptionHandlers.nonEmpty)
                exceptionHandlers.mkString(",exceptionHandlers=(\n\t", ",\n\t", "\n)")
            else
                ""
        s"$taCodeType($txtParams,$txtStmts,cfg=$cfg$txtExceptionHandlers$additionalParameters)"
    }

    /**
     * Gives for a bytecode program counter the next index in this TACode that is *not* a
     * CaughtException statement.
     */
    def properStmtIndexForPC(pc: PC): Int = {
        /*
         * There is no caught exception instruction in bytecode, so in the three-address code, a
         * CaughtException stmt will have the same pc as the next proper stmt. pcToIndex in this
         * case returns the index of the CaughtException, but analyses will usually need the index
         * of the next proper stmt.
         *
         * Example:
         * void foo() {
         *     try {
         *         ...
         *     } catch (Exception e) {
         *         e.printStackTrace();
         *     }
         * }
         *
         * In TAC:
         * 12: pc=52 caught java.lang.Exception ...
         * 13: pc=52 java.lang.Exception.printStackTrace()
         */

        val index = pcToIndex(pc)
        if (index < 0) index
        else if (stmts(index).isCaughtException) index + 1
        else index
    }
}

object TACode {

    type TACodeCFG[V <: Var[V]] = CFG[Stmt[V], TACStmts[V]]

    final val KindId = 1003

    def unapply[P <: AnyRef, V <: Var[V]](
        code: TACode[P, V]
    ): Some[(Parameters[P], Array[Stmt[V]], Array[Int], TACodeCFG[V], ExceptionHandlers)] = {
        Some((
            code.params,
            code.stmts,
            code.pcToIndex,
            code.cfg,
            code.exceptionHandlers
        ))
    }

}

final class AITACode[P <: AnyRef, VI <: ValueInformation](
        val params:            Parameters[P],
        val stmts:             Array[Stmt[DUVar[VI]]],
        val pcToIndex:         Array[Int],
        val cfg:               CFG[Stmt[DUVar[VI]], TACStmts[DUVar[VI]]],
        val exceptionHandlers: ExceptionHandlers
) extends TACode[P, DUVar[VI]] {

    import AITACode.AITACodeCFG

    /** Detaches the 3-address code from the underlying abstract interpreation result. */
    def detach(): AITACode[P, ValueInformation] = {
        new AITACode[P, ValueInformation](
            params,
            this.stmts.map(_.toCanonicalForm),
            pcToIndex,
            cfg.asInstanceOf[AITACodeCFG[ValueInformation]],
            exceptionHandlers
        )
    }

    override def toString: String = toString("AITACode", "")

}

object AITACode {

    type AITACodeCFG[VI <: ValueInformation] = CFG[Stmt[DUVar[VI]], TACStmts[DUVar[VI]]]
    type AITACodeStmts[VI <: ValueInformation] = Array[Stmt[DUVar[VI]]]

    def unapply[P <: AnyRef, VI <: ValueInformation](
        code: AITACode[P, VI]
    ): Some[(Parameters[P], AITACodeStmts[VI], Array[Int], AITACodeCFG[VI], ExceptionHandlers)] = {
        Some((
            code.params,
            code.stmts,
            code.pcToIndex,
            code.cfg,
            code.exceptionHandlers
        ))
    }

}

final class NaiveTACode[P <: AnyRef](
        val params:            Parameters[P],
        val stmts:             Array[Stmt[IdBasedVar]],
        val pcToIndex:         Array[Int],
        val cfg:               CFG[Stmt[IdBasedVar], TACStmts[IdBasedVar]],
        val exceptionHandlers: ExceptionHandlers
) extends TACode[P, IdBasedVar] {

    override def toString: String = toString("NaiveTACode", "")

}

