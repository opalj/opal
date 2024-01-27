/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package intraprocedural

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.FinalEP

/**
 * The `IntraproceduralArrayInterpreter` is responsible for processing [[ArrayLoad]] as well as
 * [[ArrayStore]] expressions in an intraprocedural fashion.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class IntraproceduralArrayInterpreter(
    cfg:         CFG[Stmt[V], TACStmts[V]],
    exprHandler: IntraproceduralInterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = ArrayLoad[V]

    /**
     * @note For this implementation, `defSite` does not play a role.
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): FinalEP[T, StringConstancyProperty] = {
        val stmts = cfg.code.instructions
        val defSites = instr.arrayRef.asVar.definedBy.toArray
        var scis = Seq.empty[StringConstancyInformation]

        defSites.filter(_ >= 0).sorted.foreach { defSite =>
            stmts(defSite).asAssignment.targetVar.usedBy.toArray.sorted.foreach {
                stmts(_) match {
                    // Process ArrayStores
                    case ArrayStore(_, _, _, value) =>
                        scis = scis ++ value.asVar.definedBy.toArray.sorted.map {
                            exprHandler.processDefSite(_).p.stringConstancyInformation
                        }
                    // Process ArrayLoads
                    case Assignment(_, _, expr: ArrayLoad[V]) =>
                        scis = scis ++ expr.arrayRef.asVar.definedBy.toArray.sorted.map {
                            exprHandler.processDefSite(_).p.stringConstancyInformation
                        }
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
