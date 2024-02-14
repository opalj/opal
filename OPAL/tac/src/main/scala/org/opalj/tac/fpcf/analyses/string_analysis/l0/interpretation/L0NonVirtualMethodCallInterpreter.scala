/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DependingStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[NonVirtualMethodCall]]s without a call graph.
 *
 * @author Maximilian Rüsch
 */
case class L0NonVirtualMethodCallInterpreter[State <: L0ComputationState[State]](
    exprHandler: InterpretationHandler[State]
) extends L0StringInterpreter[State] with DependingStringInterpreter[State] {

    implicit val _exprHandler: InterpretationHandler[State] = exprHandler

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
            case _        => NoIPResult
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
            case 0 => NoIPResult
            case _ =>
                val resultsWithPC = init.params.head.asVar.definedBy.toList.map { ds: Int =>
                    (pcOfDefSite(ds)(state.tac.stmts), handleDependentDefSite(ds))
                }
                if (resultsWithPC.forall(_._2.isFinal)) {
                    FinalIPResult(StringConstancyInformation.reduceMultiple(resultsWithPC.map(_._2.asFinal.sci)))
                } else {
                    // Some intermediate results => register necessary information from final results and return an
                    // intermediate result
                    // IMPROVE DO PROPER DEPENDENCY HANDLING
                    resultsWithPC.foreach { resultWithPC =>
                        if (resultWithPC._2.isFinal) {
                            state.appendToFpe2Sci(resultWithPC._1, resultWithPC._2.asFinal.sci, reset = true)
                        }
                    }
                    InterimIPResult.lb
                }
        }
    }
}
