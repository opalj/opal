/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds
package taint

import org.opalj.br.ObjectType
import org.opalj.ifds.AbstractIFDSFact
import org.opalj.ifds.AbstractIFDSNullFact
import org.opalj.ifds.Callable
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V

trait TaintFact extends AbstractIFDSFact

case object TaintNullFact extends TaintFact with AbstractIFDSNullFact

/**
 * A tainted variable.
 *
 * @param index The variable's definition site.
 */
case class Variable(index: Int) extends TaintFact

/**
 * A tainted array element.
 *
 * @param index The array's definition site.
 * @param element The index of the tainted element in the array.
 */
case class ArrayElement(index: Int, element: Int) extends TaintFact

/**
 * A tainted static field.
 *
 * @param classType The field's class.
 * @param fieldName The field's name.
 */
case class StaticField(classType: ObjectType, fieldName: String) extends TaintFact

/**
 * A tainted instance field.
 *
 * @param index The definition site of the field's value.
 * @param classType The field's type.
 * @param fieldName The field's value.
 */
case class InstanceField(index: Int, classType: ObjectType, fieldName: String) extends TaintFact

/**
 * A path of method calls, originating from the analyzed method, over which a tainted variable
 * reaches the sink.
 *
 * @param flow A sequence of method calls, originating from but not including this method.
 */
case class FlowFact(flow: Seq[Callable]) extends TaintFact {
    override val hashCode: Int = {
        var r = 1
        flow.foreach(f => r = (r + f.hashCode()) * 31)
        r
    }
}

/**
 * This trait represents IFDS based taint problems.
 * It decides where sanitization of taints happens.
 *
 * @tparam C the kind of callee in the problem
 * @tparam Statement the type of statements that are analyzed
 * @tparam IFDSFact the kind of generated facts
 *
 * @author Marc Clement
 */
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
