/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package interpretation
package interprocedural

import scala.collection.mutable.ListBuffer

import org.opalj.br.cfg.CFG
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP

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
                                     cfg:         CFG[Stmt[SEntity], TACStmts[SEntity]],
                                     exprHandler: InterproceduralInterpretationHandler,
                                     state:       InterproceduralComputationState,
                                     params:      List[Seq[StringConstancyInformation]]
) extends AbstractStringInterpreter(cfg, exprHandler) {

    override type T = ArrayLoad[SEntity]

    /**
     * @note This implementation will extend [[state.fpe2sci]] in a way that it adds the string
     *       constancy information for each definition site where it can compute a final result. All
     *       definition sites producing a refineable result will have to be handled later on to
     *       not miss this information.
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        val results = ListBuffer[EOptionP[Entity, StringConstancyProperty]]()

        val defSites = instr.arrayRef.asVar.definedBy.toArray
        val allDefSites = ArrayPreparationInterpreter.getStoreAndLoadDefSites(
            instr,
            state.tac.stmts
        )

        allDefSites.map { ds => (ds, exprHandler.processDefSite(ds)) }.foreach {
            case (ds, ep) =>
                if (ep.isFinal) {
                    val p = ep.asFinal.p.asInstanceOf[StringConstancyProperty]
                    state.appendToFpe2Sci(ds, p.stringConstancyInformation)
                }
                results.append(ep)
        }

        // Add information of parameters
        defSites.filter(_ < 0).foreach { ds =>
            val paramPos = Math.abs(ds + 2)
            // lb is the fallback value
            val sci = StringConstancyInformation.reduceMultiple(params.map(_(paramPos)))
            state.appendToFpe2Sci(ds, sci)
        }

        // If there is at least one InterimResult, return one. Otherwise, return a final result
        // (to either indicate that further computation are necessary or a final result is already
        // present)
        val interims = results.find(!_.isFinal)
        if (interims.isDefined) {
            interims.get
        } else {
            var resultSci = StringConstancyInformation.reduceMultiple(results.map {
                _.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
            })
            // It might be that there are no results; in such a case, set the string information to
            // the lower bound and manually add an entry to the results list
            if (resultSci.isTheNeutralElement) {
                resultSci = StringConstancyInformation.lb
            }
            if (results.isEmpty) {
                results.append(FinalEP(
                    (instr.arrayRef.asVar, state.entity._2),
                    StringConstancyProperty(resultSci)
                ))
            }

            state.appendToFpe2Sci(defSite, resultSci)
            results.head
        }
    }

}

object ArrayPreparationInterpreter {

    type T = ArrayLoad[SEntity]

    /**
     * This function retrieves all definition sites of the array stores and array loads that belong
     * to the given instruction.
     *
     * @param instr The [[ArrayLoad]] instruction to get the definition sites for.
     * @param stmts The set of statements to use.
     * @return Returns all definition sites associated with the array stores and array loads of the
     *         given instruction. The result list is sorted in ascending order.
     */
    def getStoreAndLoadDefSites(instr: T, stmts: Array[Stmt[SEntity]]): List[Int] = {
        val allDefSites = ListBuffer[Int]()
        val defSites = instr.arrayRef.asVar.definedBy.toArray

        defSites.filter(_ >= 0).sorted.foreach { next =>
            val arrDecl = stmts(next)
            val sortedArrDeclUses = arrDecl.asAssignment.targetVar.usedBy.toArray.sorted
            // For ArrayStores
            sortedArrDeclUses.filter {
                stmts(_).isInstanceOf[ArrayStore[SEntity]]
            } foreach { f: Int => allDefSites.appendAll(stmts(f).asArrayStore.value.asVar.definedBy.toArray) }
            // For ArrayLoads
            sortedArrDeclUses.filter {
                stmts(_) match {
                    case Assignment(_, _, _: ArrayLoad[SEntity]) => true
                    case _                                 => false
                }
            } foreach { f: Int =>
                val defs = stmts(f).asAssignment.expr.asArrayLoad.arrayRef.asVar.definedBy
                allDefSites.appendAll(defs.toArray)
            }
        }

        allDefSites.sorted.toList
    }

}
