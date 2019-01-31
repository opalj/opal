/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Result
import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.fpcf.analyses.string_analysis.V

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
    override def interpret(instr: T, defSite: Int): ProperPropertyComputationResult = {
        val stmts = cfg.code.instructions
        val children = ListBuffer[StringConstancyInformation]()
        // Loop over all possible array values
        val defSites = instr.arrayRef.asVar.definedBy.toArray
        defSites.filter(_ >= 0).sorted.foreach { next ⇒
            val arrDecl = stmts(next)
            val sortedArrDeclUses = arrDecl.asAssignment.targetVar.usedBy.toArray.sorted
            // Process ArrayStores
            sortedArrDeclUses.filter {
                stmts(_).isInstanceOf[ArrayStore[V]]
            } foreach { f: Int ⇒
                val sortedDefs = stmts(f).asArrayStore.value.asVar.definedBy.toArray.sorted
                children.appendAll(sortedDefs.map { exprHandler.processDefSite }.map { n ⇒
                    val r = n.asInstanceOf[Result]
                    r.finalEP.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                })
            }
            // Process ArrayLoads
            sortedArrDeclUses.filter {
                stmts(_) match {
                    case Assignment(_, _, _: ArrayLoad[V]) ⇒ true
                    case _                                 ⇒ false
                }
            } foreach { f: Int ⇒
                val defs = stmts(f).asAssignment.expr.asArrayLoad.arrayRef.asVar.definedBy
                children.appendAll(defs.toArray.sorted.map { exprHandler.processDefSite }.map { n ⇒
                    val r = n.asInstanceOf[Result]
                    r.finalEP.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                })
            }
        }

        // In case it refers to a method parameter, add a dynamic string property
        if (defSites.exists(_ < 0)) {
            children.append(StringConstancyProperty.lb.stringConstancyInformation)
        }

        Result(instr, StringConstancyProperty(
            StringConstancyInformation.reduceMultiple(
                children.filter(!_.isTheNeutralElement).toList
            )
        ))
    }

}
