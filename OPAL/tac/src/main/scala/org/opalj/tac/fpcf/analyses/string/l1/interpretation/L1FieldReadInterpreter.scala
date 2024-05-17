/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package l1
package interpretation

import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.FieldAccessInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeDynamicString
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeNull
import org.opalj.br.fpcf.properties.string.StringTreeOr
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
import org.opalj.tac.fpcf.analyses.string.interpretation.InterpretationHandler
import org.opalj.tac.fpcf.analyses.string.l1.L1StringAnalysis
import org.opalj.tac.fpcf.properties.string.ConstantResultFlow
import org.opalj.tac.fpcf.properties.string.StringFlowFunction

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
) extends AssignmentBasedStringInterpreter {

    override type E = FieldRead[V]

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
        var target:                PV,
        var hasWriteInSameMethod:  Boolean                                          = false,
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
     * [[org.opalj.br.fpcf.properties.string.StringConstancyLevel.DYNAMIC]].
     */
    override def interpretExpr(target: V, fieldRead: E)(implicit
        state: InterpretationState
    ): ProperPropertyComputationResult = {
        if (!StringAnalysis.isSupportedType(fieldRead.declaredFieldType)) {
            return computeFinalLBFor(target)
        }

        val definedField =
            declaredFields(fieldRead.declaringClass, fieldRead.name, fieldRead.declaredFieldType).asDefinedField
        val writeAccesses = fieldAccessInformation.writeAccesses(definedField.definedField).toSeq
        if (writeAccesses.length > fieldWriteThreshold) {
            return computeFinalLBFor(target)
        }

        val persistentTarget = target.toPersistentForm(state.tac.stmts)
        if (writeAccesses.isEmpty) {
            // No methods which write the field were found => Field could either be null or any value
            return computeFinalResult(ConstantResultFlow.forVariable(
                persistentTarget,
                StringTreeOr.fromNodes(StringTreeNull, StringTreeDynamicString)
            ))
        }

        implicit val accessState: FieldReadState = FieldReadState(persistentTarget)
        writeAccesses.foreach {
            case (contextId, _, _, parameter) =>
                val method = contextProvider.contextFromId(contextId).method.definedMethod

                if (method == state.dm.definedMethod) {
                    accessState.hasWriteInSameMethod = true
                }

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

    private def tryComputeFinalResult(implicit
        accessState: FieldReadState,
        state:       InterpretationState
    ): ProperPropertyComputationResult = {
        if (accessState.hasWriteInSameMethod) {
            // We cannot handle writes to a field that is read in the same method at the moment as the flow functions do
            // not capture field state. This can be improved upon in the future.
            computeFinalLBFor(accessState.target)
        } else if (accessState.hasDependees) {
            InterimResult.forUB(
                InterpretationHandler.getEntity,
                StringFlowFunction.ub,
                accessState.dependees.toSet,
                continuation(accessState, state)
            )
        } else {
            var trees = accessState.accessDependees.map(_.asFinal.p.sci.tree)
            // No init is present => append a `null` element to indicate that the field might be null; this behavior
            // could be refined by only setting the null element if no statement is guaranteed to be executed prior
            // to the field read
            if (!accessState.hasInit) {
                trees = trees :+ StringTreeNull
            }
            // If an access could not be resolved, append a dynamic element
            if (accessState.hasUnresolvableAccess) {
                trees = trees :+ StringTreeNode.lb
            }

            computeFinalResult(ConstantResultFlow.forVariable(accessState.target, StringTreeNode.reduceMultiple(trees)))
        }
    }

    private def continuation(
        accessState: FieldReadState,
        state:       InterpretationState
    )(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(_: StringConstancyProperty) =>
                accessState.updateAccessDependee(eps.asInstanceOf[EOptionP[SContext, StringConstancyProperty]])
                tryComputeFinalResult(accessState, state)

            case _ => throw new IllegalArgumentException(s"Encountered unknown eps: $eps")
        }
    }
}
