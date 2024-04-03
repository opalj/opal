/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string_analysis
package l1
package interpretation

import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.StringConstancyProperty
import org.opalj.br.fpcf.properties.string_definition.StringConstancyInformation
import org.opalj.br.fpcf.properties.string_definition.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string_definition.StringTreeNull
import org.opalj.br.fpcf.properties.string_definition.StringTreeOr
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.log.Error
import org.opalj.log.Info
import org.opalj.log.OPALLogger.logOnce
import org.opalj.tac.fpcf.analyses.string_analysis.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string_analysis.l1.L1StringAnalysis

/**
 * Responsible for processing direct reads to fields (see [[FieldRead]]) by analyzing the write accesses to these fields
 * via the [[FieldAccessInformation]].
 *
 * @author Maximilian Rüsch
 */
case class L1FieldReadInterpreter(
    ps:                           PropertyStore,
    fieldAccessInformation:       FieldAccessInformation,
    project:                      SomeProject,
    implicit val declaredFields:  DeclaredFields,
    implicit val contextProvider: ContextProvider
) extends StringInterpreter {

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

    private case class FieldReadState(
        defSitePC:                 Int,
        state:                     DefSiteState,
        var hasInit:               Boolean                                          = false,
        var hasUnresolvableAccess: Boolean                                          = false,
        var accessDependees:       Seq[EOptionP[SContext, StringConstancyProperty]] = Seq.empty
    ) {

        def updateAccessDependee(newDependee: EOptionP[SContext, StringConstancyProperty]): Unit = {
            accessDependees = accessDependees.updated(
                accessDependees.indexWhere(_.e == newDependee.e),
                newDependee
            )
        }

        def hasDependees: Boolean = accessDependees.exists(_.isRefinable)

        def dependees: Iterable[SomeEOptionP] = accessDependees.filter(_.isRefinable)
    }

    /**
     * Currently, fields are approximated using the following approach: If a field of a type not supported by the
     * [[L1StringAnalysis]] is passed, [[StringConstancyInformation.lb]] will be produces. Otherwise, all write accesses
     * are considered and analyzed. If a field is not initialized within a constructor or the class itself, it will be
     * approximated using all write accesses as well as with the lower bound and "null" => in these cases fields are
     * [[org.opalj.br.fpcf.properties.string_definition.StringConstancyLevel.DYNAMIC]].
     */
    override def interpret(instr: T, pc: Int)(implicit state: DefSiteState): ProperPropertyComputationResult = {
        // TODO: The approximation of fields might be outsourced into a dedicated analysis. Then, one could add a
        //  finer-grained processing or provide different abstraction levels. This analysis could then use that analysis.
        if (!StringAnalysis.isSupportedType(instr.declaredFieldType)) {
            return computeFinalResult(pc, StringConstancyInformation.lb)
        }

        val definedField = declaredFields(instr.declaringClass, instr.name, instr.declaredFieldType).asDefinedField
        val writeAccesses = fieldAccessInformation.writeAccesses(definedField.definedField).toSeq
        if (writeAccesses.length > fieldWriteThreshold) {
            return computeFinalResult(pc, StringConstancyInformation.lb)
        }

        if (writeAccesses.isEmpty) {
            // No methods which write the field were found => Field could either be null or any value
            return computeFinalResult(
                pc,
                StringConstancyInformation(
                    tree = StringTreeOr.fromNodes(StringTreeNull, StringTreeDynamicString)
                )
            )
        }

        implicit val accessState: FieldReadState = FieldReadState(pc, state)
        writeAccesses.foreach {
            case (contextId, _, _, parameter) =>
                val method = contextProvider.contextFromId(contextId).method.definedMethod

                if (method.name == "<init>" || method.name == "<clinit>") {
                    accessState.hasInit = true
                }

                if (parameter.isEmpty) {
                    // Field parameter information is not available
                    accessState.hasUnresolvableAccess = true
                } else {
                    val entity: SContext = (PUVar(parameter.get._1, parameter.get._2), method)
                    accessState.accessDependees = accessState.accessDependees :+ ps(entity, StringConstancyProperty.key)
                }
        }

        tryComputeFinalResult
    }

    private def tryComputeFinalResult(implicit accessState: FieldReadState): ProperPropertyComputationResult = {
        if (accessState.hasDependees) {
            InterimResult.forUB(
                InterpretationHandler.getEntityForPC(accessState.defSitePC)(accessState.state),
                StringConstancyProperty.ub,
                accessState.dependees.toSet,
                continuation(accessState)
            )
        } else {
            var scis = accessState.accessDependees.map(_.asFinal.p.sci)
            // No init is present => append a `null` element to indicate that the field might be null; this behavior
            // could be refined by only setting the null element if no statement is guaranteed to be executed prior
            // to the field read
            if (!accessState.hasInit) {
                scis = scis :+ StringConstancyInformation.nullElement
            }
            // If an access could not be resolved, append a dynamic element
            if (accessState.hasUnresolvableAccess) {
                scis = scis :+ StringConstancyInformation.lb
            }

            computeFinalResult(accessState.defSitePC, StringConstancyInformation.reduceMultiple(scis))(accessState.state)
        }
    }

    private def continuation(accessState: FieldReadState)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(_: StringConstancyProperty) =>
                accessState.updateAccessDependee(eps.asInstanceOf[EOptionP[SContext, StringConstancyProperty]])
                tryComputeFinalResult(accessState)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }
}
