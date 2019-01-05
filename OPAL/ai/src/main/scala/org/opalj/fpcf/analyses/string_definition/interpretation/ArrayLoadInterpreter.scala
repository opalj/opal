/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.analyses.string_definition.interpretation
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.analyses.string_definition.V
import org.opalj.fpcf.string_definition.properties.StringConstancyInformation
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.properties.StringConstancyProperty

/**
 * The `ArrayLoadInterpreter` is responsible for processing [[ArrayLoad]] expressions.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class ArrayLoadInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterpretationHandler
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = ArrayLoad[V]

    /**
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T): List[StringConstancyInformation] = {
        val stmts = cfg.code.instructions
        val children = ListBuffer[StringConstancyInformation]()
        // Loop over all possible array values
        val defSites = instr.arrayRef.asVar.definedBy.toArray
        defSites.filter(_ >= 0).sorted.foreach { next ⇒
            val arrDecl = stmts(next)
            val sortedArrDeclUses = arrDecl.asAssignment.targetVar.usedBy.toArray.sorted
            sortedArrDeclUses.filter {
                stmts(_).isInstanceOf[ArrayStore[V]]
            } foreach { f: Int ⇒
                val sortedDefs = stmts(f).asArrayStore.value.asVar.definedBy.toArray.sorted
                sortedDefs.map { exprHandler.processDefSite }.foreach {
                    children.appendAll(_)
                }
            }
        }

        // In case it refers to a method parameter, add a dynamic string property
        if (defSites.exists(_ < 0)) {
            children.append(StringConstancyProperty.lowerBound.stringConstancyInformation)
        }

        children.toList
    }

}
