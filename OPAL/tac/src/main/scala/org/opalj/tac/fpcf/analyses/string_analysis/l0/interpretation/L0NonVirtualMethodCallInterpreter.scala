/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l0
package interpretation

import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.DependingStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[NonVirtualMethodCall]]s without a call graph.
 * For supported method calls, see the documentation of the `interpret` function.
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
     * For all other calls, a result containing [[StringConstancyInformation.getNeutralElement]] will be returned.
     */
    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
        val sciOpt = instr.name match {
            case "<init>" => interpretInit(instr)
            case _        => Some(StringConstancyInformation.getNeutralElement)
        }
        // IMPROVE DO PROPER DEPENDENCY HANDLING
        FinalEP(instr, StringConstancyProperty(sciOpt.getOrElse(StringConstancyInformation.lb)))
    }

    /**
     * Processes an `&lt;init&gt;` method call. If it has no parameters,
     * [[StringConstancyProperty.getNeutralElement]] will be returned. Otherwise, only the very
     * first parameter will be evaluated and its result returned (this is reasonable as both,
     * [[StringBuffer]] and [[StringBuilder]], have only constructors with <= 1 arguments and only
     * these are currently interpreted).
     */
    private def interpretInit(init: T)(implicit state: State): Option[StringConstancyInformation] = {
        init.params.size match {
            case 0 => Some(StringConstancyInformation.getNeutralElement)
            case _ =>
                val sciOptsWithPC = init.params.head.asVar.definedBy.toList.map { ds: Int =>
                    (pcOfDefSite(ds)(state.tac.stmts), handleDependentDefSite(ds))
                }
                if (sciOptsWithPC.forall(_._2.isDefined)) {
                    Some(StringConstancyInformation.reduceMultiple(sciOptsWithPC.map(_._2.get)))
                } else {
                    // Some intermediate results => register necessary information from final results and return an
                    // intermediate result
                    sciOptsWithPC.foreach { sciOptWithPC =>
                        if (sciOptWithPC._2.isDefined) {
                            state.appendToFpe2Sci(sciOptWithPC._1, sciOptWithPC._2.get, reset = true)
                        }
                    }
                    None
                }
        }
    }
}
