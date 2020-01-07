/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.string_analysis.interpretation.interprocedural

import scala.collection.mutable.ListBuffer

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.tac.fpcf.analyses.string_analysis.V
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.AbstractStringInterpreter
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralComputationState
import org.opalj.tac.fpcf.analyses.string_analysis.InterproceduralStringAnalysis
import org.opalj.tac.FieldRead
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.Stmt

/**
 * The `InterproceduralFieldInterpreter` is responsible for processing instances of [[FieldRead]]s.
 * At this moment, this includes instances of [[PutField]] and [[PutStatic]]. For the processing
 * procedure, see [[InterproceduralFieldInterpreter#interpret]].
 *
 * @see [[AbstractStringInterpreter]]
 *
 * @author Patrick Mell
 */
class InterproceduralFieldInterpreter(
        state:                  InterproceduralComputationState,
        exprHandler:            InterproceduralInterpretationHandler,
        ps:                     PropertyStore,
        fieldAccessInformation: FieldAccessInformation
) extends AbstractStringInterpreter(state.tac.cfg, exprHandler) {

    override type T = FieldRead[V]

    /**
     * Currently, fields are approximated using the following approach. If a field of a type not
     * supported by the [[InterproceduralStringAnalysis]] is passed,
     * [[StringConstancyInformation.lb]] will be produces. Otherwise, all write accesses are
     * considered and analyzed. If a field is not initialized within a constructor or the class
     * itself, it will be approximated using all write accesses as well as with the lower bound and
     * "null" => in these cases fields are [[StringConstancyLevel.DYNAMIC]].
     *
     * @note For this implementation, `defSite` plays a role!
     *
     * @see [[AbstractStringInterpreter.interpret]]
     */
    override def interpret(instr: T, defSite: Int): EOptionP[Entity, StringConstancyProperty] = {
        // TODO: The approximation of fields might be outsourced into a dedicated analysis. Then,
        //  one could add a finer-grained processing or provide different abstraction levels. This
        //  String analysis could then use the field analysis.
        val defSitEntity: Integer = defSite
        // Unknown type => Cannot further approximate
        if (!InterproceduralStringAnalysis.isSupportedType(instr.declaredFieldType)) {
            return FinalEP(instr, StringConstancyProperty.lb)
        }
        // Write accesses exceeds the threshold => approximate with lower bound
        val writeAccesses = fieldAccessInformation.writeAccesses(instr.declaringClass, instr.name)
        if (writeAccesses.length > state.fieldWriteThreshold) {
            return FinalEP(instr, StringConstancyProperty.lb)
        }

        var hasInit = false
        val results = ListBuffer[EOptionP[Entity, StringConstancyProperty]]()
        writeAccesses.foreach {
            case (m, pcs) ⇒ pcs.foreach { pc ⇒
                if (m.name == "<init>" || m.name == "<clinit>") {
                    hasInit = true
                }
                val (tacEps, tac) = getTACAI(ps, m, state)
                val nextResult = if (tacEps.isRefinable) {
                    EPK(state.entity, StringConstancyProperty.key)
                } else {
                    tac match {
                        case Some(methodTac) ⇒
                            val stmt = methodTac.stmts(methodTac.pcToIndex(pc))
                            val entity = (extractUVarFromPut(stmt), m)
                            val eps = ps(entity, StringConstancyProperty.key)
                            if (eps.isRefinable) {
                                state.dependees = eps :: state.dependees
                                // We need some mapping from an entity to an index in order for
                                // the processFinalP to find an entry. We cannot use the given
                                // def site as this would mark the def site as finalized even
                                // though it might not be. Thus, we use -1 as it is a safe dummy
                                // value
                                state.appendToVar2IndexMapping(entity._1, -1)
                            }
                            eps
                        case _ ⇒
                            // No TAC available
                            FinalEP(defSitEntity, StringConstancyProperty.lb)
                    }
                }
                results.append(nextResult)
            }
        }

        if (results.isEmpty) {
            // No methods, which write the field, were found => Field could either be null or
            // any value
            val possibleStrings = "(^null$|"+StringConstancyInformation.UnknownWordSymbol+")"
            val sci = StringConstancyInformation(
                StringConstancyLevel.DYNAMIC, possibleStrings = possibleStrings
            )
            state.appendToFpe2Sci(
                defSitEntity, StringConstancyProperty.lb.stringConstancyInformation
            )
            FinalEP(defSitEntity, StringConstancyProperty(sci))
        } else {
            // If all results are final, determine all possible values for the field. Otherwise,
            // return some intermediate result to indicate that the computation is not yet done
            if (results.forall(_.isFinal)) {
                // No init is present => append a `null` element to indicate that the field might be
                // null; this behavior could be refined by only setting the null element if no
                // statement is guaranteed to be executed prior to the field read
                if (!hasInit) {
                    results.append(FinalEP(
                        instr, StringConstancyProperty(StringConstancyInformation.getNullElement)
                    ))
                }
                val finalSci = StringConstancyInformation.reduceMultiple(results.map {
                    _.asFinal.p.asInstanceOf[StringConstancyProperty].stringConstancyInformation
                })
                state.appendToFpe2Sci(defSitEntity, finalSci)
                FinalEP(defSitEntity, StringConstancyProperty(finalSci))
            } else {
                results.find(!_.isFinal).get
            }
        }
    }

    /**
     * This function extracts a DUVar from a given statement which is required to be either of type
     * [[PutStatic]] or [[PutField]].
     */
    private def extractUVarFromPut(field: Stmt[V]): V = field match {
        case PutStatic(_, _, _, _, value)   ⇒ value.asVar
        case PutField(_, _, _, _, _, value) ⇒ value.asVar
        case _                              ⇒ throw new IllegalArgumentException(s"Type of $field is currently not supported!")
    }

}
