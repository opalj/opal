/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.tac.{Assignment, Expr, Stmt}
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V

trait TaintProblem[C, Statement, IFDSFact] {

    /**
     * Checks, if some `callee` is a sanitizer, which sanitizes its return value.
     * In this case, no return flow facts will be created.
     *
     * @param callee The method, which was called.
     * @return True, if the method is a sanitizer.
     */
    protected def sanitizesReturnValue(callee: C): Boolean

    /**
     * Called in callToReturnFlow. This method can return whether the input fact
     * will be removed after `callee` was called. I.e. the method could sanitize parameters.
     *
     * @param call The call statement.
     * @param in The fact which holds before the call.
     * @return Whether in will be removed after the call.
     */
    protected def sanitizesParameter(call: Statement, in: IFDSFact): Boolean
}

object TaintProblem {

    /**
     * Checks, if some expression always evaluates to the same int constant.
     *
     * @param expression The expression.
     * @param code The TAC code, which contains the expression.
     * @return Some int, if this analysis is sure that `expression` always evaluates to the same int
     *         constant, None otherwise.
     */
    def getIntConstant(expression: Expr[V], code: Array[Stmt[V]]): Option[Int] = {
        if (expression.isIntConst) Some(expression.asIntConst.value)
        else if (expression.isVar) {
            val definedByIterator = expression.asVar.definedBy.iterator
            var allDefinedByWereConstant = true
            var result = scala.collection.mutable.Seq.empty[Int]
            while (definedByIterator.hasNext && allDefinedByWereConstant) {
                val definedBy = definedByIterator.next()
                if (definedBy >= 0) {
                    val stmt = code(definedBy)
                    if (stmt.astID == Assignment.ASTID && stmt.asAssignment.expr.isIntConst)
                        result :+= stmt.asAssignment.expr.asIntConst.value
                    else allDefinedByWereConstant = false
                } else allDefinedByWereConstant = false
            }
            if (allDefinedByWereConstant && result.tail.forall(_ == result.head))
                Some(result.head)
            else None
        } else None
    }
}