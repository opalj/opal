/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyType
import org.opalj.br.fpcf.properties.string_definition.StringTreeConst
import org.opalj.fpcf.ProperPropertyComputationResult

/**
 * @author Maximilian Rüsch
 */
object SimpleValueConstExprInterpreter extends StringInterpreter {

    override type T = SimpleValueConst

    def interpret(expr: T, pc: Int)(implicit state: DefSiteState): ProperPropertyComputationResult = {
        val treeOpt = expr match {
            case ic: IntConst    => Some(StringTreeConst(ic.value.toString))
            case fc: FloatConst  => Some(StringTreeConst(fc.value.toString))
            case dc: DoubleConst => Some(StringTreeConst(dc.value.toString))
            case lc: LongConst   => Some(StringTreeConst(lc.value.toString))
            case sc: StringConst => Some(StringTreeConst(sc.value))
            case _               => None
        }

        val sci = treeOpt
            .map(StringConstancyInformation(StringConstancyType.APPEND, _))
            .getOrElse(StringConstancyInformation.neutralElement)
        computeFinalResult(pc, sci)
    }
}
