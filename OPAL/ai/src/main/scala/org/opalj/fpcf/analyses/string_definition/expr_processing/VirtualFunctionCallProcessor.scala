/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.expr_processing
import org.opalj.tac.Expr
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.properties.StringConstancyLevel.CONSTANT
import org.opalj.fpcf.properties.StringConstancyLevel.DYNAMIC
import org.opalj.fpcf.properties.StringConstancyLevel.PARTIALLY_CONSTANT
import org.opalj.fpcf.properties.StringConstancyLevel.StringConstancyLevel
import org.opalj.fpcf.properties.StringConstancyProperty
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.VirtualFunctionCall

import scala.collection.mutable.ArrayBuffer

/**
 * This implementation of [[AbstractExprProcessor]] processes [[org.opalj.tac.VirtualFunctionCall]]
 * expressions.
 * Currently, [[VirtualFunctionCallProcessor]] (only) aims at processing calls of
 * [[StringBuilder#append]].
 *
 * @author Patrick Mell
 */
class VirtualFunctionCallProcessor() extends AbstractExprProcessor {

    /**
     * `expr` is required to be of type [[org.opalj.tac.VirtualFunctionCall]] (otherwise `None` will
     * be returned).
     *
     * @see [[AbstractExprProcessor.process]]
     */
    override def process(stmts: Array[Stmt[V]], expr: Expr[V]): Option[StringConstancyProperty] = {
        var level = CONSTANT

        expr match {
            case vfc: VirtualFunctionCall[V] ⇒
                val receiver = vfc.receiver
                // TODO: Are these long concatenations the best / most robust way?
                val appendCall =
                    stmts(receiver.asVar.definedBy.head).asAssignment.expr.asVirtualFunctionCall

                // Get previous value of string builder
                val baseAssignment = stmts(appendCall.receiver.asVar.definedBy.head).asAssignment
                val baseStr = valueOfAppendCall(baseAssignment.expr.asVirtualFunctionCall, stmts)
                var assignedStr = baseStr._1
                // Get appended value and build the new string value
                val appendData = valueOfAppendCall(appendCall, stmts)
                if (appendData._2 == CONSTANT) {
                    assignedStr += appendData._1
                } else {
                    assignedStr += "*"
                    level = PARTIALLY_CONSTANT
                }

                Some(StringConstancyProperty(level, ArrayBuffer(assignedStr)))
            case _ ⇒ None
        }
    }

    /**
     * Determines the string value that was passed to a `StringBuilder#append` method. This function
     * can process string constants as well as function calls as argument to append.
     *
     * @param call  A function call of `StringBuilder#append`. Note that for all other methods an
     *              [[IllegalArgumentException]] will be thrown.
     * @param stmts The surrounding context, e.g., the surrounding method.
     * @return For constants strings as arguments, this function returns the string value and the
     *         level [[org.opalj.fpcf.properties.StringConstancyLevel.CONSTANT]]. For function calls
     *         "*" (to indicate ''any value'') and
     *         [[org.opalj.fpcf.properties.StringConstancyLevel.DYNAMIC]].
     */
    private def valueOfAppendCall(
        call: VirtualFunctionCall[V], stmts: Array[Stmt[V]]
    ): (String, StringConstancyLevel) = {
        // TODO: Check the base object as well
        if (call.name != "append") {
            throw new IllegalArgumentException("can only process StringBuilder#append calls")
        }

        val defAssignment = call.params.head.asVar.definedBy.head
        val assignExpr = stmts(defAssignment).asAssignment.expr
        assignExpr match {
            case _: NonVirtualFunctionCall[V] ⇒ Tuple2("*", DYNAMIC)
            case StringConst(_, value)        ⇒ (value, CONSTANT)
        }
    }

}
