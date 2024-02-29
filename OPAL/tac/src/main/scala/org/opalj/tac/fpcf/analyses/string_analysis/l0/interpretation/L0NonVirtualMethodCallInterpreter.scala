/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[NonVirtualMethodCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0NonVirtualMethodCallInterpreter[State <: L0ComputationState](
    exprHandler: InterpretationHandler[State]
) extends L0StringInterpreter[State] with IPResultDependingStringInterpreter[State] {

    override type T = NonVirtualMethodCall[V]

    /**
     * Currently, this function supports the interpretation of the following non virtual methods:
     * <ul>
     * <li>
     * `&lt;init&gt;`, when initializing an object (for this case, currently zero constructor or
     * one constructor parameter are supported; if more params are available, only the very first
     * one is interpreted).
     * </li>
     * </ul>
     *
     * For all other calls, a [[NoIPResult]] will be returned.
     */
    override def interpret(instr: T, defSite: Int)(implicit state: State): IPResult = {
        instr.name match {
            case "<init>" => interpretInit(instr)
            case _        => NoIPResult(state.dm, instr.pc)
        }
    }

    /**
     * Processes an `&lt;init&gt;` method call. If it has no parameters, [[NoIPResult]] will be returned. Otherwise,
     * only the very first parameter will be evaluated and its result returned (this is reasonable as both,
     * [[StringBuffer]] and [[StringBuilder]], have only constructors with <= 1 arguments and only these are currently
     * interpreted).
     */
    private def interpretInit(init: T)(implicit state: State): IPResult = {
        init.params.size match {
            case 0 => NoIPResult(state.dm, init.pc)
            case _ =>
                val results = init.params.head.asVar.definedBy.toList.map(exprHandler.processDefSite)
                if (results.forall(_.isFinal)) {
                    finalResult(init.pc)(results)
                } else {
                    InterimIPResult.lbWithIPResultDependees(
                        state.dm,
                        init.pc,
                        results.filter(_.isRefinable).asInstanceOf[Iterable[RefinableIPResult]],
                        awaitAllFinalContinuation(
                            SimpleIPResultDepender(init, init.pc, state, results),
                            finalResult(init.pc)
                        )
                    )
                }
        }
    }

    private def finalResult(pc: Int)(results: Iterable[IPResult])(implicit state: State): FinalIPResult = {
        FinalIPResult(StringConstancyInformation.reduceMultiple(results.map(_.asFinal.sci)), state.dm, pc)
    }
}
