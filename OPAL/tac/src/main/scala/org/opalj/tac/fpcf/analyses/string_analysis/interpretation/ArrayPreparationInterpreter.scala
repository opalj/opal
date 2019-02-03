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
import org.opalj.tac.fpcf.analyses.string_analysis.ComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.V

/**
 * The `ArrayPreparationInterpreter` is responsible for preparing [[ArrayLoad]] as well as
 * [[ArrayStore]] expressions in an interprocedural fashion.
 * <p>
 * Not all (partial) results are guaranteed to be available at once, thus intermediate results
 * might be produced. This interpreter will only compute the parts necessary to later on fully
 * assemble the final result for the array interpretation.
 * For more information, see the [[interpret]] method.
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class ArrayPreparationInterpreter(
        cfg:         CFG[Stmt[V], TACStmts[V]],
        exprHandler: InterproceduralInterpretationHandler,
        state:       ComputationState,
        params:      List[StringConstancyInformation]
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = ArrayLoad[V]

    /**
     * @note This implementation will extend [[state.fpe2sci]] in a way that it adds the string
     *       constancy information foreach definition site where it can compute a final result. All
     *       definition sites producing an intermediate result will have to be handled later on to
     *       not miss this information.
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): ProperPropertyComputationResult = {
        val stmts = cfg.code.instructions
        val results = ListBuffer[ProperPropertyComputationResult]()

        // Loop over all possible array values
        val allDefSites = ListBuffer[Int]()
        val defSites = instr.arrayRef.asVar.definedBy.toArray
        defSites.filter(_ >= 0).sorted.foreach { next ⇒
            val arrDecl = stmts(next)
            val sortedArrDeclUses = arrDecl.asAssignment.targetVar.usedBy.toArray.sorted
            // Process ArrayStores
            sortedArrDeclUses.filter {
                stmts(_).isInstanceOf[ArrayStore[V]]
            } foreach { f: Int ⇒
                allDefSites.appendAll(stmts(f).asArrayStore.value.asVar.definedBy.toArray)
            }
            // Process ArrayLoads
            sortedArrDeclUses.filter {
                stmts(_) match {
                    case Assignment(_, _, _: ArrayLoad[V]) ⇒ true
                    case _                                 ⇒ false
                }
            } foreach { f: Int ⇒
                val defs = stmts(f).asAssignment.expr.asArrayLoad.arrayRef.asVar.definedBy
                allDefSites.appendAll(defs.toArray)
            }
        }

        allDefSites.sorted.map { ds ⇒ (ds, exprHandler.processDefSite(ds)) }.foreach {
            case (ds, r: Result) ⇒
                state.appendResultToFpe2Sci(ds, r)
                results.append(r)
            case (_, ir: ProperPropertyComputationResult) ⇒ results.append(ir)
        }

        // Add information of parameters
        defSites.filter(_ < 0).foreach { ds ⇒
            val paramPos = Math.abs(defSite + 2)
            // lb is the fallback value
            var sci = StringConstancyInformation(
                possibleStrings = StringConstancyInformation.UnknownWordSymbol
            )
            if (paramPos < params.size) {
                sci = params(paramPos)
            }
            val e: Integer = ds
            state.appendResultToFpe2Sci(ds, Result(e, StringConstancyProperty(sci)))
        }

        // If there is at least one InterimResult, return one. Otherwise, return a final result
        // (to either indicate that further computation are necessary or a final result is already
        // present)
        val interimResult = results.find(!_.isInstanceOf[Result])
        if (interimResult.isDefined) {
            interimResult.get
        } else {
            results.head
        }
    }

}
