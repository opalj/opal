/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * The `IntraproceduralStaticFunctionCallInterpreter` is responsible for processing
 * [[StaticFunctionCall]]s in an intraprocedural fashion.
 * <p>
 * For supported method calls, see the documentation of the `interpret` function.
 *
 * @author Maximilian Rüsch
 */
case class L0StaticFunctionCallInterpreter[State <: ComputationState[State]](
        override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        override protected val exprHandler: InterpretationHandler[State]
) extends L0StringInterpreter[State] {

    override type T = StaticFunctionCall[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] =
        FinalEP(instr, StringConstancyProperty.lb)
}
