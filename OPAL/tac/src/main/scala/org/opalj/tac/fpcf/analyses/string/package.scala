/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.Context

/**
 * @author Maximilian Rüsch
 */
package object string {

    type TAC = TACode[TACMethodParameter, V]

    trait AnalyzableVariable {
        def pc: Int
        def pv: PV
        def m: Method
    }

    private[string] case class VariableDefinition(pc: Int, pv: PV, m: Method) extends AnalyzableVariable
    case class VariableContext(pc: Int, pv: PV, context: Context) extends AnalyzableVariable {
        override def m: Method = context.method.definedMethod
    }

    case class MethodPC(pc: Int, dm: DefinedMethod)
}
