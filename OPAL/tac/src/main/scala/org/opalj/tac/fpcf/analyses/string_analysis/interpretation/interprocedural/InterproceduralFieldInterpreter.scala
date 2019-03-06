/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperOnUpdateContinuation
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.tac.GetField
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralStringAnalysis

/**
 * The `InterproceduralFieldInterpreter` is responsible for processing [[GetField]]s. In this
 * implementation, there is currently only primitive support for fields, i.e., they are not analyzed
 * but a constant [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation]]
 * is returned (see [[interpret]] of this class).
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralFieldInterpreter(
        state:                  InterproceduralComputationState,
        exprHandler:            InterproceduralInterpretationHandler,
        ps:                     PropertyStore,
        fieldAccessInformation: FieldAccessInformation,
        c:                      ProperOnUpdateContinuation
) extends AbstractStringInterpreter(state.tac.cfg, exprHandler) {

    override type T = GetField[V]

    /**
     * Currently, fields are not interpreted. Thus, this function always returns a list with a
     * single element consisting of
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.DYNAMIC]],
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyType.APPEND]] and
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation.UnknownWordSymbol]].
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): ProperPropertyComputationResult = {
        val defSitEntity: Integer = defSite
        if (InterproceduralStringAnalysis.isSupportedType(instr.declaredFieldType)) {
            val results = ListBuffer[ProperPropertyComputationResult]()
            fieldAccessInformation.writeAccesses(instr.declaringClass, instr.name).foreach {
                case (m, pcs) ⇒ pcs.foreach { pc ⇒
                    getTACAI(ps, m, state) match {
                        case Some(methodTac) ⇒
                            val stmt = methodTac.stmts(methodTac.pcToIndex(pc))
                            val entity = (stmt.asPutField.value.asVar, m)
                            val eps = ps(entity, StringConstancyProperty.key)
                            results.append(eps match {
                                case FinalEP(e, p) ⇒ Result(e, p)
                                case _ ⇒
                                    state.dependees = eps :: state.dependees
                                    state.appendToVar2IndexMapping(entity._1, defSite)
                                    InterimResult(
                                        entity,
                                        StringConstancyProperty.lb,
                                        StringConstancyProperty.ub,
                                        List(),
                                        c
                                    )
                            })
                        case _ ⇒
                            state.appendToFpe2Sci(
                                defSitEntity, StringConstancyProperty.lb.stringConstancyInformation
                            )
                            results.append(Result(defSitEntity, StringConstancyProperty.lb))
                    }
                }
            }
            if (results.isEmpty) {
                // No methods, which write the field, were found => Field could either be null or
                // any value
                val possibleStrings = "(^null$|"+StringConstancyInformation.UnknownWordSymbol+")"
                val sci = StringConstancyInformation(
                    StringConstancyLevel.DYNAMIC, possibleStrings = possibleStrings
                )
                Result(defSitEntity, StringConstancyProperty(sci))
            } else {
                // If available, return an intermediate result to indicate that the computation
                // needs to be continued
                results.find(!_.isInstanceOf[Result]).getOrElse(results.head)
            }
        } else {
            // Unknown type => Cannot further approximate
            Result(instr, StringConstancyProperty.lb)
        }
    }

}
