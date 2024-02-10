/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import scala.collection.mutable.ListBuffer

import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStore
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.tac.fpcf.analyses.string_analysis.l1.L1StringAnalysis

/**
 * Responsible for processing direct reads to fields (see [[FieldRead]]) by analyzing the write accesses to these fields
 * via the [[FieldAccessInformation]].
 *
 * @author Maximilian Rüsch
 */
case class L1FieldReadInterpreter[State <: L1ComputationState[State]](
        ps:                           PropertyStore,
        fieldAccessInformation:       FieldAccessInformation,
        project:                      SomeProject,
        implicit val declaredFields:  DeclaredFields,
        implicit val contextProvider: ContextProvider
) extends L1StringInterpreter[State] {

    override type T = FieldRead[V]

    /**
     * To analyze a read operation of field, ''f'', all write accesses, ''wa_f'', to ''f'' have to be analyzed.
     * ''fieldWriteThreshold'' determines the threshold of ''|wa_f|'' when ''f'' is to be approximated as the lower bound.
     */
    private val fieldWriteThreshold = {
        val threshold =
            try {
                project.config.getInt(L1StringAnalysis.FieldWriteThresholdConfigKey)
            } catch {
                case t: Throwable =>
                    logOnce(Error(
                        "analysis configuration - l1 string analysis",
                        s"couldn't read: ${L1StringAnalysis.FieldWriteThresholdConfigKey}",
                        t
                    ))(project.logContext)
                    10
            }

        logOnce(Info(
            "analysis configuration - l1 string analysis",
            "l1 string analysis uses a field write threshold of " + threshold
        ))(project.logContext)
        threshold
    }

    /**
     * Currently, fields are approximated using the following approach: If a field of a type not supported by the
     * [[L1StringAnalysis]] is passed, [[StringConstancyInformation.lb]] will be produces. Otherwise, all write accesses
     * are considered and analyzed. If a field is not initialized within a constructor or the class itself, it will be
     * approximated using all write accesses as well as with the lower bound and "null" => in these cases fields are
     * [[StringConstancyLevel.DYNAMIC]].
     */
    override def interpret(instr: T, defSite: Int)(implicit state: State): EOptionP[Entity, StringConstancyProperty] = {
        // TODO: The approximation of fields might be outsourced into a dedicated analysis. Then, one could add a
        //  finer-grained processing or provide different abstraction levels. This analysis could then use that analysis.
        if (!StringAnalysis.isSupportedType(instr.declaredFieldType)) {
            return FinalEP(instr, StringConstancyProperty.lb)
        }

        val definedField = declaredFields(instr.declaringClass, instr.name, instr.declaredFieldType).asDefinedField
        val writeAccesses = fieldAccessInformation.writeAccesses(definedField.definedField).toSeq
        if (writeAccesses.length > fieldWriteThreshold) {
            return FinalEP(instr, StringConstancyProperty.lb)
        }

        var hasInit = false
        val results = ListBuffer[EOptionP[Entity, StringConstancyProperty]]()
        writeAccesses.foreach {
            case (contextId, _, _, parameter) =>
                val method = contextProvider.contextFromId(contextId).method.definedMethod

                if (method.name == "<init>" || method.name == "<clinit>") {
                    hasInit = true
                }
                val (tacEps, tac) = getTACAI(ps, method, state)
                val nextResult = if (parameter.isEmpty) {
                    // Field parameter information is not available
                    FinalEP(defSite.asInstanceOf[Integer], StringConstancyProperty.lb)
                } else if (tacEps.isRefinable) {
                    EPK(state.entity, StringConstancyProperty.key)
                } else {
                    tac match {
                        case Some(_) =>
                            val entity = (PUVar(parameter.get._1, parameter.get._2), method)
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
                        case _ =>
                            // No TAC available
                            FinalEP(defSite.asInstanceOf[Integer], StringConstancyProperty.lb)
                    }
                }
                results.append(nextResult)
        }

        if (results.isEmpty) {
            // No methods which write the field were found => Field could either be null or any value
            val sci = StringConstancyInformation(
                StringConstancyLevel.DYNAMIC,
                possibleStrings =
                    s"(${StringConstancyInformation.NullStringValue}|${StringConstancyInformation.UnknownWordSymbol})"
            )
            state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), StringConstancyInformation.lb)
            FinalEP(defSite.asInstanceOf[Integer], StringConstancyProperty(sci))
        } else {
            if (results.forall(_.isFinal)) {
                // No init is present => append a `null` element to indicate that the field might be null; this behavior
                // could be refined by only setting the null element if no statement is guaranteed to be executed prior
                // to the field read
                if (!hasInit) {
                    results.append(FinalEP(
                        instr,
                        StringConstancyProperty(StringConstancyInformation.getNullElement)
                    ))
                }
                val finalSci = StringConstancyInformation.reduceMultiple(results.map {
                    _.asFinal.p.stringConstancyInformation
                })
                state.appendToFpe2Sci(pcOfDefSite(defSite)(state.tac.stmts), finalSci)
                FinalEP(defSite.asInstanceOf[Integer], StringConstancyProperty(finalSci))
            } else {
                results.find(!_.isFinal).get
            }
        }
    }
}
