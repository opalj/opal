/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSFact
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSNullFact
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.Assignment
import org.opalj.tac.Expr
import org.opalj.tac.Stmt
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V

trait Fact extends AbstractIFDSFact

case object NullFact extends Fact with AbstractIFDSNullFact

/**
 * A tainted variable.
 *
 * @param index The variable's definition site.
 */
case class Variable(index: Int) extends Fact

/**
 * A tainted array element.
 *
 * @param index The array's definition site.
 * @param element The index of the tainted element in the array.
 */
case class ArrayElement(index: Int, element: Int) extends Fact

/**
 * A tainted static field.
 *
 * @param classType The field's class.
 * @param fieldName The field's name.
 */
case class StaticField(classType: ObjectType, fieldName: String) extends Fact

/**
 * A tainted instance field.
 *
 * @param index The definition site of the field's value.
 * @param classType The field's type.
 * @param fieldName The field's value.
 */
case class InstanceField(index: Int, classType: ObjectType, fieldName: String) extends Fact

/**
 * A path of method calls, originating from the analyzed method, over which a tainted variable
 * reaches the sink.
 *
 * @param flow A sequence of method calls, originating from but not including this method.
 */
case class FlowFact(flow: Seq[Method]) extends Fact {
    override val hashCode: Int = {
        var r = 1
        flow.foreach(f ⇒ r = (r + f.hashCode()) * 31)
        r
    }
}

trait TaintAnalysis {

    /**
     * Checks, if some `callee` is a sanitizer, which sanitizes its return value.
     * In this case, no return flow facts will be created.
     *
     * @param callee The method, which was called.
     * @return True, if the method is a sanitizer.
     */
    protected def sanitizesReturnValue(callee: DeclaredMethod): Boolean

    /**
     * Called in callToReturnFlow. This method can return facts, which will be removed after
     * `callee` was called. I.e. the method could sanitize parameters.
     *
     * @param call The call statement.
     * @param in The facts, which hold before the call.
     * @return Facts, which will be removed from `in` after the call.
     */
    protected def sanitizeParamters(call: Statement, in: Set[Fact]): Set[Fact]
}

object TaintAnalysis {

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