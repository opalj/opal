/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformationConst
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * @author Maximilian Rüsch
 */
object SimpleValueConstExprInterpreter extends StringInterpreter {

    override type T = SimpleValueConst

    def interpret(expr: T, pc: Int)(implicit state: DUSiteState): ProperPropertyComputationResult = {
        val sci = expr match {
            case ic: IntConst    => StringConstancyInformationConst(StringTreeConst(ic.value.toString))
            case fc: FloatConst  => StringConstancyInformationConst(StringTreeConst(fc.value.toString))
            case dc: DoubleConst => StringConstancyInformationConst(StringTreeConst(dc.value.toString))
            case lc: LongConst   => StringConstancyInformationConst(StringTreeConst(lc.value.toString))
            case sc: StringConst => StringConstancyInformationConst(StringTreeConst(sc.value))
            case _               => StringConstancyInformation.neutralElement
        }

        computeFinalResult(pc, sci)
    }
}
