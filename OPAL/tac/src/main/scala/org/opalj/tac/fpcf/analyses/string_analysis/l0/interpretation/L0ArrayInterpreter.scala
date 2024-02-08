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
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler

/**
 * Responsible for processing [[ArrayLoad]] as well as [[ArrayStore]] expressions in an intraprocedural fashion.
 *
 * @author Maximilian Rüsch
 */
case class L0ArrayInterpreter[State <: ComputationState[State]](
        override protected val cfg:         CFG[Stmt[V], TACStmts[V]],
        override protected val exprHandler: InterpretationHandler[State]
) extends L0StringInterpreter[State] {

    override type T = ArrayLoad[V]

    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
        val stmts = cfg.code.instructions
        val defSites = instr.arrayRef.asVar.definedBy.toArray
        var scis = Seq.empty[StringConstancyInformation]

        defSites.filter(_ >= 0).sorted.foreach { defSite =>
            stmts(defSite).asAssignment.targetVar.usedBy.toArray.sorted.foreach {
                stmts(_) match {
                    // Process ArrayStores
                    case ArrayStore(_, _, _, value) =>
                        scis = scis ++ value.asVar.definedBy.toArray.sorted.flatMap { handleDependentDefSite }
                    // Process ArrayLoads
                    case Assignment(_, _, expr: ArrayLoad[V]) =>
                        scis = scis ++ expr.arrayRef.asVar.definedBy.toArray.sorted.flatMap { handleDependentDefSite }
                    case _ =>
                }
            }
        }

        // In case it refers to a method parameter, add a dynamic string property
        if (defSites.exists(_ < 0)) {
            scis = scis :+ StringConstancyInformation.lb
        }

        FinalEP(instr, StringConstancyProperty(StringConstancyInformation.reduceMultiple(scis)))
    }
}
