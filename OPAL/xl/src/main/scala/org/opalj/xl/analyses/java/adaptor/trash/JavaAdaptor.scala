/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.xl.analyses.java.adaptor.trash

import org.opalj.tac.Expr
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.fpcf.analyses.cg.V

object JavaAdaptor {
  def CrossLanguageAdaptor(
      tacai: Option[TACode[TACMethodParameter, V]],
      sfc: StaticFunctionCall[V]
  ): (String, String, Expr[V], Expr[V]) = {
    val param1 = sfc.params.head
    val param2 = sfc.params.tail.head
    val stmts = tacai.get.stmts

    val languageName = stmts(param1.asVar.definedBy.head).asAssignment.expr.asStringConst.value
    val functionName = stmts(param2.asVar.definedBy.head).asAssignment.expr.asStringConst.value
    val paramA = sfc.params.tail.tail.head
    val paramB = sfc.params.tail.tail.tail.head

    (languageName, functionName, paramA, paramB)
  }
}
